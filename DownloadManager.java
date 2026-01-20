import java.util.*;
import java.util.concurrent.*;

/*
 =====================================================
 FUNCTIONAL REQUIREMENTS
 =====================================================
 - Download multiple files
 - Support operations: start, pause, resume, cancel
 - Limit number of concurrent downloads

 NON-FUNCTIONAL REQUIREMENTS
 =====================================================
 - Thread-safe
 - Controlled concurrency
 - Maintainable & extensible design

 HIGH LEVEL DESIGN
 =====================================================
 - Each download runs as a task
 - ExecutorService manages thread pool
 - ConcurrentHashMap tracks downloads
 - State machine via DownloadStatus enum

 DESIGN PRINCIPLES & PATTERNS
 =====================================================
 - SRP: Each class has one responsibility
 - Encapsulation: DownloadTask hides download logic
 - Strategy Pattern: Runnable encapsulates execution logic
 - Facade Pattern: DownloadManager exposes simple APIs
 - Thread Pool Pattern: ExecutorService
 =====================================================
*/


// =========================
// Download Status Enum
// =========================
// - Represents finite state machine for download lifecycle
// - Type-safe state handling
enum DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}


// =========================
// DownloadTask (Worker)
// =========================
// - Runnable represents a unit of work
// - SRP: Handles only download execution
// - State is maintained internally
class DownloadTask implements Runnable {

    private final String id;
    private final String url;
    private volatile DownloadStatus status;   // volatile for visibility across threads
    private volatile int progress;

    public DownloadTask(String id, String url){
        this.id = id;
        this.url = url;
        this.status = DownloadStatus.QUEUED;
        this.progress = 0;
    }

    @Override
    public void run(){
        status = DownloadStatus.DOWNLOADING;

        try {
            // Simulate download progress
            while (progress < 100) {

                // Pause or cancel handling
                if (status == DownloadStatus.PAUSED) {
                    Thread.sleep(200);
                    continue;
                }

                if (status == DownloadStatus.CANCELLED) {
                    return;
                }

                Thread.sleep(100); // simulate network delay
                progress += 10;
            }

            status = DownloadStatus.COMPLETED;

        } catch (Exception e) {
            status = DownloadStatus.FAILED;
        }
    }

    // Pause download
    public void pause(){
        if (status == DownloadStatus.DOWNLOADING) {
            status = DownloadStatus.PAUSED;
        }
    }

    // Resume download
    public void resume(){
        if (status == DownloadStatus.PAUSED) {
            status = DownloadStatus.DOWNLOADING;
        }
    }

    // Cancel download
    public void cancel(){
        status = DownloadStatus.CANCELLED;
    }

    public DownloadStatus getStatus(){
        return status;
    }
}


// =========================
// DownloadManager (Facade)
// =========================
// - Facade Pattern: Simplifies client interaction
// - Manages lifecycle of DownloadTasks
// - Thread-safe using ConcurrentHashMap
public class DownloadManager {

    private final ExecutorService executor;
    private final Map<String, DownloadTask> downloads;

    public DownloadManager(int maxConcurrentDownloads){
        // Thread Pool Pattern
//        ExecutorService internally uses a thread-safe blocking queue + worker threads
//            , so queuing, locking, and coordination are handled for you.
        this.executor = Executors.newFixedThreadPool(maxConcurrentDownloads);
        this.downloads = new ConcurrentHashMap<>();
    }

    // Start a new download (non-blocking)
    public String startDownload(String url){
        String id = UUID.randomUUID().toString();
        DownloadTask task = new DownloadTask(id, url);

        downloads.put(id, task);
        executor.submit(task); // executes run() asynchronously
        return id;
    }

    public void pauseDownload(String id){
        DownloadTask task = downloads.get(id);
        if (task != null) {
            task.pause();
        }
    }

    public void resumeDownload(String id){
        DownloadTask task = downloads.get(id);
        if (task != null) {
            task.resume();
        }
    }

    public void cancelDownload(String id){
        DownloadTask task = downloads.get(id);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown(){
        executor.shutdown();
    }
}


// =========================
// Main (Client)
// =========================
// - Demonstrates usage
public class Main {
    public static void main(String[] args) throws InterruptedException {

        DownloadManager manager = new DownloadManager(3);

        String id1 = manager.startDownload("https://file1.com/movie.mp4");

        Thread.sleep(300);
        manager.pauseDownload(id1);

        Thread.sleep(300);
        manager.resumeDownload(id1);

        Thread.sleep(300);
        manager.cancelDownload(id1);

        manager.shutdown();
    }
}
