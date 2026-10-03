package testSuite;

import liquidjava.specification.Refinement;

public class CorrectCombinedNestedFields {
    @Refinement("_ < 0")
    int port = -1;

    private final Job job = new Job();

    static class Marker {
        int value;
    }

    @Refinement("_ < 0")
    public int getOuterPort() {
        return port;
    }

    @Refinement("_ >= 0")
    public int getJobPort() {
        return job.port;
    }

    public void send() {
        job.port = 5;
    }

    static class Job {
        @Refinement("_ >= 0")
        int port;
    }
}
