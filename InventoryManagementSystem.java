import java.util.concurrent.*;

// =========================
// Functional Requirements
// =========================
// - Add products
// - Update product quantity
// - Reserve inventory for an order
// - Release inventory (Cancel/failure)
// - Handle concurrent incoming requests
// - Requests should be queued

// =========================
// Non-functional
// =========================
// - Thread-safe
// - No race conditions
// - Scalable and asynchronous

// =========================
// Data Flow
// =========================
// Client -> RequestQueue -> InventoryService -> InventoryStore

// =========================
// Entities
// =========================
// - Product
// - InventoryStore
// - InventoryService
// - InventoryRequest (Command Pattern)

/*
 =========================
 Product (SRP)
 Manages its own state: id, quantity, name
 =========================
*/
public class Product {
    private final String productId;
    private int quantity;
    private final String name;

    public Product(String productId, String name, int quantity){
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void add(int qnt){
        this.quantity += qnt;
    }

    public void remove(int qnt) throws IllegalArgumentException {
        if(qnt > quantity){
            throw new IllegalArgumentException("Insufficient stock for product " + productId);
        }
        this.quantity -= qnt;
    }
}

/*
 =========================
 InventoryStore
 Thread-safe storage using ConcurrentHashMap
 =========================
*/
public class InventoryStore {
    private final ConcurrentHashMap<String, Product> store = new ConcurrentHashMap<>();

    public Product getProduct(String productId){
        return store.get(productId);
    }

    public void addProduct(Product product){
        store.put(product.getProductId(), product);
    }
}

/*
 =========================
 InventoryService
 Business logic: Add/Reserve/Release stock
 Thread-safe (synchronized ensures atomic updates)
 =========================
*/
public class InventoryService {
    private final InventoryStore store;

    public InventoryService(InventoryStore store){
        this.store = store;
    }

    public synchronized void addStock(String productId, int quantity){
        Product p = store.getProduct(productId);
        if(p != null){
            p.add(quantity);
        } else {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
    }

    public synchronized void reserveStock(String productId, int quantity){
        Product p = store.getProduct(productId);
        if(p != null){
            p.remove(quantity); // throws if not enough stock
        } else {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
    }

    public synchronized void releaseStock(String productId, int quantity){
        Product p = store.getProduct(productId);
        if(p != null){
            p.add(quantity);
        } else {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
    }
}

/*
 =========================
 InventoryRequest (Command Pattern)
 =========================
*/
public interface InventoryRequest {
    void process(InventoryService service);
}

/* AddStockRequest */
public class AddStockRequest implements InventoryRequest {
    private final String productId;
    private final int quantity;

    public AddStockRequest(String productId, int quantity){
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public void process(InventoryService service){
        service.addStock(productId, quantity);
        System.out.println("Added " + quantity + " to " + productId);
    }
}

/* ReserveStockRequest */
public class ReserveStockRequest implements InventoryRequest {
    private final String productId;
    private final int quantity;

    public ReserveStockRequest(String productId, int quantity){
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public void process(InventoryService service){
        service.reserveStock(productId, quantity);
        System.out.println("Reserved " + quantity + " from " + productId);
    }
}

/* RemoveStockRequest (Release stock) */
public class ReleaseStockRequest implements InventoryRequest {
    private final String productId;
    private final int quantity;

    public ReleaseStockRequest(String productId, int quantity){
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public void process(InventoryService service){
        service.releaseStock(productId, quantity);
        System.out.println("Released " + quantity + " back to " + productId);
    }
}

/*
 =========================
 RequestQueue
 - Thread-safe queue for incoming requests
 - Uses BlockingQueue to block workers when empty
 =========================
*/
public class RequestQueue {
    private final BlockingQueue<InventoryRequest> queue = new LinkedBlockingQueue<>();

    public void add(InventoryRequest request){
        queue.add(request);
    }

    public InventoryRequest take() throws InterruptedException {
        return queue.take();
    }
}

/*
 =========================
 RequestProcessor
 - Runnable worker
 - Continuously consumes requests
 - SRP, DIP, Producer-Consumer Pattern
 =========================
*/
public class RequestProcessor implements Runnable {
    private final RequestQueue queue;
    private final InventoryService service;

    public RequestProcessor(RequestQueue queue, InventoryService service){
        this.queue = queue;
        this.service = service;
    }

    @Override
    public void run() {
        while(true){
            try{
                InventoryRequest request = queue.take(); // blocks if queue empty
                request.process(service); // delegates business logic
            } catch (Exception e){
                System.out.println("Failed to process request: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

/*
 =========================
 Main
 - Demonstrates system in action
 - Starts multiple worker threads
 =========================
*/
public class Main {
    public static void main(String[] args){
        InventoryStore store = new InventoryStore();
        store.addProduct(new Product("P1", "Product1", 100));

        InventoryService service = new InventoryService(store);
        RequestQueue queue = new RequestQueue();

        // Start 4 worker threads
        for(int i = 0; i < 4; i++){
            Thread worker = new Thread(new RequestProcessor(queue, service));
            worker.start();
        }

        // Add requests
        queue.add(new AddStockRequest("P1", 50));
        queue.add(new ReserveStockRequest("P1", 30));
        queue.add(new ReleaseStockRequest("P1", 20));
    }
}
