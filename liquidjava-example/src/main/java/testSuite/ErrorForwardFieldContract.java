import liquidjava.specification.Refinement;

public class ErrorForwardFieldContract {
    private final Job job = new Job();
    
    public void send() {
        job.port = -1; // Expect: Refinement Error
    }
    
    static class Job {
        @Refinement("_ >= 0")
        int port;
    }
}
