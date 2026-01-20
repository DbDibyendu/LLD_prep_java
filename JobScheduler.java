
// Functional req
// Schedule a job at a specific time
// Recurring Jobs?
// Cancellation job
// Execute the jobs when their scheduled time arrives

// core entities
// Job
// Scheduler
// JobStore
// WorkerPool
// TriggerPoint

// class diagram in java
public interface Job {
    void execute();
}

public class ScheduledJob {
    private String id;
    private Job job;
    private Instant nextTime;
    private long durationMillis;

    public:
    ScheduledJob(String id, Job job, long durationMillis) {
        this.id = id;
        this.job = job;
        this.durationMillis = durationMillis;
    }

    boolean isRecurring(){}

    void updateNextTime(){}

    Job getJob(){
    }

    String getId(){
    }
}


//ExecutorService is a Java concurrency framework abstraction that manages a
//    pool of threads and executes tasks asynchronously, providing better performance, control,
//    and safety than manually managing threads
public class WorkerPool {
  
    private ExecutorService executor;

    public WorkerPool(int poolSize) {
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    public void submitJob(Job job) {
      executorService.submit(job::execute);
    }
}


public class PrintJob Implmements Job {
    private String message;

    PrintJob(String message) {
        this.message = message;
    }

    @Override
    public void execute() {
        System.out.println("Executing Job: " + message);
    }
}

