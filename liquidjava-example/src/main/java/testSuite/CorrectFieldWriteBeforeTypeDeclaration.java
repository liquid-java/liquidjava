public class CorrectFieldWriteBeforeTypeDeclaration {
    private final Job job = new Job();
    
    public void send(int port) {
        job.port = port;
    }
    
    static class Job {
        int port;
    }
}
