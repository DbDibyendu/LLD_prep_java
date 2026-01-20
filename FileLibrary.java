import java.util.*;
import java.nio.file.*;
import java.io.IOException;

// ==================== FUNCTIONAL REQUIREMENTS ====================
//
// 1. The system should read files from a given directory path.
//
// 3. The system should support filtering files based on:
//    - File size range (min size, max size)
//    - File extension (e.g. .txt, .log)
//
// 4. The system should allow combining multiple filters together
//    (e.g. size + extension).
//
// 5. The filtering logic should be extensible without modifying
//    existing search or scan logic.
//
// 6. The directory scanning logic should be separated from
//    filtering logic.
//
// 7. Filters should be applied dynamically at runtime
//    (pluggable strategies).
//
// 8. The system should not apply file-based filters to directories.
//
//
// ==================== CORE ENTITIES ====================
//
// 1. FileInfo
//    - Represents metadata of a file or directory
//    - Fields: path, size, extension, isDirectory
//
// 2. Filter (Interface)
//    - Abstraction for all filtering strategies
//
// 3. SizeFilter (implements filter)
//    - Filters files based on size constraints
//
// 4. ExtensionFilter (implements filter)
//    - Filters files based on file extension
//
// 5. CompositeFilter  (implements filter)
//    - Combines multiple filters using logical AND
//
// 6. DirectoryScanner (implements filter)
//    - Reads the filesystem and constructs FileInfo objects
//
// 7. FileSearchEngine
//    - Orchestrates directory scanning and filter application
//
/*
 =========================
  FileInfo (Entity)
 =========================
 - SRP (Single Responsibility Principle):
   Represents metadata of a file only.
 - Encapsulation:
   Fields are private, accessed via getters.
*/
class FileInfo {
    private final String path;
    private final long size;
    private final String extension;
    private final boolean isDirectory;

    public FileInfo(String path, long size, String extension, boolean isDirectory) {
        this.path = path;
        this.size = size;
        this.extension = extension;
        this.isDirectory = isDirectory;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isDirectory() {
        return isDirectory;
    }
}

/*
 =========================
  Filter Interface
 =========================
 - OCP (Open/Closed Principle):
   New filters can be added without modifying existing code.
 - DIP (Dependency Inversion Principle):
   High-level modules depend on abstraction, not concrete filters.
 - Strategy Pattern:
   Each filter is a strategy for filtering files.
   Abstraction:  client doesn't need to know apply implementation details.
*/
interface Filter {
    boolean apply(FileInfo file);
}

/*
 =========================
  SizeFilter
 =========================
 - SRP:
   Only checks size constraints.
 - Polymorphism:
   Implements Filter interface.
*/
class SizeFilter implements Filter {
    private final long minSize;
    private final long maxSize;

    public SizeFilter(long minSize, long maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    @Override
    public boolean apply(FileInfo file) {
        return !file.isDirectory()
                && file.getSize() >= minSize
                && file.getSize() <= maxSize;
    }
}

/*
 =========================
  ExtensionFilter
 =========================
 - SRP:
   Only checks extension.
*/
class ExtensionFilter implements Filter {
    private final String extension;

    public ExtensionFilter(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean apply(FileInfo file) {
        return !file.isDirectory()
                && file.getExtension().equalsIgnoreCase(extension);
    }
}

/*
 =========================
  CompositeFilter
 =========================
 - Composite Pattern:
   Combines multiple filters.
 - OCP:
   New filters can be added without changing search logic.
*/
class CompositeFilter implements Filter {
    private final List<Filter> filters;

    public CompositeFilter(List<Filter> filters) {
        this.filters = filters;
    }

    @Override
    public boolean apply(FileInfo file) {
        for (Filter filter : filters) {
            if (!filter.apply(file)) {
                return false;
            }
        }
        return true;
    }
}

/*
 =========================
  DirectoryScanner
 =========================
 - SRP:
   Responsible only for reading filesystem.
 - Low-level module.
*/
class DirectoryScanner {

    public List<FileInfo> scan(String directoryPath) throws IOException {
        List<FileInfo> files = new ArrayList<>();

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(Paths.get(directoryPath))) {

            for (Path entry : stream) {
                files.add(new FileInfo(
                        entry.toString(),
                        Files.isDirectory(entry) ? 0 : Files.size(entry),
                        getExtension(entry),
                        Files.isDirectory(entry)
                ));
            }
        }
        return files;
    }

    private String getExtension(Path path) {
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        return idx == -1 ? "" : name.substring(idx);
    }
}

/*
 =========================
  FileSearchEngine
 =========================
 - SRP:
   Orchestrates scanning + filtering.
 - DIP:
   Depends on Filter abstraction.
 - Strategy Pattern:
   Filter strategy injected at runtime.
*/
class FileSearchEngine {
    private final DirectoryScanner scanner;

    public FileSearchEngine(DirectoryScanner scanner) {
        this.scanner = scanner;
    }

    public List<FileInfo> search(String path, Filter filter) throws IOException {
        List<FileInfo> files = scanner.scan(path);
        List<FileInfo> result = new ArrayList<>();

        for (FileInfo file : files) {
            if (filter.apply(file)) {
                result.add(file);
            }
        }
        return result;
    }
}

/*
 =========================
  Main
 =========================
 - Demonstrates:
   * Polymorphism
   * Strategy Pattern
   * Dependency Injection
*/
public class Main {
    public static void main(String[] args) throws IOException {

        DirectoryScanner scanner = new DirectoryScanner();
        FileSearchEngine searchEngine = new FileSearchEngine(scanner);

        Filter sizeFilter = new SizeFilter(1000, 5000);
        Filter extensionFilter = new ExtensionFilter(".txt");

        Filter combinedFilter = new CompositeFilter(
                Arrays.asList(sizeFilter, extensionFilter)
        );

        List<FileInfo> results =
                searchEngine.search("/your/directory/path", combinedFilter);

        for (FileInfo file : results) {
            System.out.println(file.getPath());
        }
    }
}
