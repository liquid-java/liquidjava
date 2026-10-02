package testSuite.classes.forward_field_read_correct;

import liquidjava.specification.Refinement;

public class ReadBefore {
    private final Job job = new Job();

    @Refinement("_ >= 0")
    public int get() {
        return job.port;
    }

    static class Job {
        @Refinement("_ >= 0")
        int port;
    }
}
