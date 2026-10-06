package testSuite;

import liquidjava.specification.Refinement;

public class CorrectDistinctClassFields {
    @Refinement("_ < 0") int port = -1;
    private final Job job = new Job();

    public void send() {
        job.port = 5;
    }

    static class Job {
        @Refinement("_ >= 0") int port;
    }
}
