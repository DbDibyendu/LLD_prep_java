// System should limit requests per user.
// N requests per some time window. Exceeding requests should be rejected.
// good to have
// system should support different algorithms
// system should be thread safe.

// Entities
//RateLimiter
//RateLimitStrategy
//UserRequestInfo
//TimeWindow

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * STRATEGY PATTERN
 * ----------------
 * RateLimitStrategy defines a family of algorithms (FixedWindow, TokenBucket, etc.)
 * and makes them interchangeable at runtime.
 *
 * SOLID:
 * - OCP (Open/Closed): New rate limiting algorithms can be added without modifying clients.
 * - DIP (Dependency Inversion): High-level RateLimiter depends on abstraction, not concrete class.
 */
interface RateLimitStrategy {
    boolean allowRequest(String userId);
}

/*
 * FIXED WINDOW RATE LIMITER
 *
 * Uses time windows like:
 * - Allow N requests per windowSizeMs
 *
 * Tradeoff:
 * - Burst allowed at window boundary (classic fixed window problem)
 */
class FixedWindowRateLimiter implements RateLimitStrategy {

    private final int limit;
    private final long windowSizeMs;

    /*
     * ConcurrentHashMap ensures thread-safe access at map level
     * without global locking.
     *
     * CONCURRENCY:
     * - Fine-grained locking (per-user Window object)
     */
    private final Map<String, Window> store = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int limit, long windowSizeMs) {
        this.limit = limit;
        this.windowSizeMs = windowSizeMs;
    }

    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();

        /*
         * THREAD SAFETY
         * -------------
         * putIfAbsent is atomic in ConcurrentHashMap
         */
        store.putIfAbsent(userId, new Window(now, 0));
        Window window = store.get(userId);

        /*
         * SYNCHRONIZATION
         * ----------------
         * Synchronizing on per-user window avoids global lock
         * and ensures correctness for count + time reset.
         */
        synchronized (window) {
            if (now - window.startTime >= windowSizeMs) {
                window.startTime = now;
                window.count = 1;
                return true;
            }

            if (window.count < limit) {
                window.count++;
                return true;
            }

            return false;
        }
    }
}

/*
 * SLIDING WINDOW (placeholder)
 *
 * OCP:
 * - Can be implemented later without touching RateLimiter or clients.
 */
class SlidingWindowRateLimiter implements RateLimitStrategy {
    @Override
    public boolean allowRequest(String userId) {
        return true; // to be implemented
    }
}

/*
 * TOKEN BUCKET RATE LIMITER
 *
 * Smooths traffic and avoids burst problem of fixed window.
 */
class TokenBucketRateLimiter implements RateLimitStrategy {

    private final int capacity;
    private final int refillRate;

    /*
     * CONCURRENCY:
     * - ConcurrentHashMap avoids contention across users
     */
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
    }

    @Override
    public boolean allowRequest(String userId) {

        /*
         * Lazy initialization
         * -------------------
         * Buckets are created only when a user makes a request.
         */
        buckets.putIfAbsent(userId, new TokenBucket(capacity));
        return buckets.get(userId).tryConsume(refillRate);
    }
}

/*
 * TOKEN BUCKET
 *
 * Encapsulates token refill + consumption logic.
 *
 * SRP (Single Responsibility):
 * - This class ONLY manages tokens.
 */
class TokenBucket {

    private int tokens;
    private final int capacity;
    private long lastRefillTime;

    TokenBucket(int capacity) {
        this.tokens = capacity;
        this.capacity = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    /*
     * SYNCHRONIZED METHOD
     * -------------------
     * Ensures atomic refill + consume operation.
     */
    synchronized boolean tryConsume(int refillRate) {
        refill(refillRate);
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }

    /*
     * Time-based refill
     */
    private void refill(int refillRate) {
        long now = System.currentTimeMillis();
        long seconds = (now - lastRefillTime) / 1000;

        if (seconds > 0) {
            tokens = Math.min(tokens + (int) (seconds * refillRate), capacity);
            lastRefillTime = now;
        }
    }
}

/*
 * WINDOW DATA HOLDER
 *
 * SRP:
 * - Holds only window state (startTime, count)
 */
class Window {
    long startTime;
    int count;

    Window(long startTime, int count) {
        this.startTime = startTime;
        this.count = count;
    }
}

/*
 * RATE LIMITER (CONTEXT)
 *
 * STRATEGY PATTERN – Context
 * --------------------------
 * Delegates rate limiting decision to the chosen strategy.
 *
 * SOLID:
 * - DIP: Depends on RateLimitStrategy interface
 */
class RateLimiter {

    private final RateLimitStrategy strategy;

    public RateLimiter(RateLimitStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean allowRequest(String userId) {
        return strategy.allowRequest(userId);
    }
}

/*
 * CLIENT
 *
 * Demonstrates runtime strategy selection.
 */
public class Main {

    public static void main(String[] args) {

        /*
         * STRATEGY SELECTION AT RUNTIME
         */
        RateLimitStrategy strategy =
                new FixedWindowRateLimiter(5, 1000);

        RateLimiter limiter = new RateLimiter(strategy);

        for (int i = 0; i < 10; i++) {
            boolean allowed = limiter.allowRequest("user1");
            System.out.println("Request " + (i + 1) +
                    ": " + (allowed ? "Allowed" : "Blocked"));
        }
    }
}
