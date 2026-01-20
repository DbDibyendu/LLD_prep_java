import java.time.Instant;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 =========================
  Functional Requirements
 =========================
 - Schedule a job at a specific time
 - Support recurring jobs
 - Allow job cancellation (can be extended)
 - Execute jobs when their scheduled time arrives
*/

/*
 =========================
  Core Entities
 =========================
 - Job
 - ScheduledJob
 - JobScheduler
 - JobStore
 - WorkerPool
*/


// ---------------- JOB ----------------

// Command Pattern:
// Job encapsulates an executable action
public interface Job {
    void execute();
}


// ---------------- SCHEDULED JOB ----------------

// Entity Object
// SRP: Holds scheduling metadata only
class ScheduledJob implements Comparable<ScheduledJob> {

    private String id;
    private Job job;
    private Instant nextTime;
    private long intervalMillis; // 0 => non-recurring

    public ScheduledJob(String id, Job job, Instant nextTime, long intervalMillis) {
        this.id = id;
        this.job = job;
        this.nextTime = nextTime;
        this.intervalMillis = intervalMillis;
    }

    boolean isRecurring() {
        return intervalMillis > 0;
    }

    void updateNextTime() {
        this.nextTime = nextTime.plusMillis(intervalMillis);
    }

    Instant getNextTime() {
        return nextTime;
    }

    Job getJob() {
        return job;
    }

    String getId() {
        return id;
    }

    // PriorityQueue ordering → earliest execution first
    @Override
    public int compareTo(ScheduledJob other) {
        return this.nextTime.compareTo(other.nextTime);
    }
}


// ---------------- WORKER POOL ----------------

// ExecutorService is a Java concurrency framework abstraction that manages a
// pool of threads and executes tasks asynchronously, providing better performance,
// control, and safety than manually managing threads
class WorkerPool {

    private ExecutorService executorService;

    // Thread Pool Pattern
    public WorkerPool(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public void submitJob(Job job) {
        executorService.submit(job::execute);
    }
}


// ---------------- JOB STORE ----------------

// SRP: Responsible only for job storage
// Uses PriorityQueue (Min-Heap) ordered by execution time
class JobStore {

    private PriorityQueue<ScheduledJob> queue = new PriorityQueue<>();

    public synchronized void schedule(ScheduledJob job) {
        queue.offer(job);
    }

    public synchronized ScheduledJob peek() {
        return queue.peek();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    // Removes and returns the job with the earliest execution time
    public synchronized ScheduledJob poll() {
        return queue.poll();
    }
}


// ---------------- JOB SCHEDULER ----------------

// Orchestrator / Facade
// SRP: Coordinates scheduling + execution
// DIP: Depends on abstractions (Job, WorkerPool)
class JobScheduler {

    private JobStore jobStore;
    private WorkerPool workerPool;

    public JobScheduler(JobStore jobStore, WorkerPool workerPool) {
        this.jobStore = jobStore;
        this.workerPool = workerPool;
        start();
    }

    public void schedule(ScheduledJob job) {
        jobStore.schedule(job);
    }

    // Dedicated scheduler thread
    private void start() {

        Thread schedulerThread = new Thread(() -> {
            while (true) {
                try {

                    ScheduledJob job = jobStore.peek();

                    if (job == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    long delay =
                            job.getNextTime().toEpochMilli() - Instant.now().toEpochMilli();

                    if (delay > 0) {
                        Thread.sleep(Math.min(delay, 100));
                        continue;
                    }

                    // Time reached → execute
                    jobStore.poll();
                    workerPool.submitJob(job.getJob());

                    // Reschedule if recurring
                    if (job.isRecurring()) {
                        job.updateNextTime();
                        jobStore.schedule(job);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }
}


// ---------------- SAMPLE JOB ----------------

// Concrete Command
class PrintJob implements Job {

    private String message;

    PrintJob(String message) {
        this.message = message;
    }

    @Override
    public void execute() {
        System.out.println("Executing Job: " + message +
                " at " + Instant.now());
    }
}


// ---------------- MAIN ----------------
//        The scheduler runs as a long-lived background thread that continuously checks the earliest
//        scheduled job using a priority queue. It calculates the time difference and sleeps until execution
//        time arrives, avoiding CPU busy-waiting. Once the time is reached, it delegates execution
//        to a worker pool and reschedules the job if it’s recurring

public class Main {
    public static void main(String[] args) {

        JobStore store = new JobStore();
        WorkerPool pool = new WorkerPool(5);
        JobScheduler scheduler = new JobScheduler(store, pool);

        ScheduledJob job = new ScheduledJob(
                "job-1",
                new PrintJob("Hello Scheduler"),
                Instant.now().plusSeconds(2),
                0 // non-recurring
        );

        scheduler.schedule(job);
    }
}
