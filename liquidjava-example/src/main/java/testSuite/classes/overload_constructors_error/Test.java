package testSuite.classes.overload_constructors_error;

public class Test{
    void test3(){
        Throwable t = new Throwable("Example");
        t.initCause(null);
        t.getCause();
    }

    void test4(){
        Throwable originT = new Throwable();
        Throwable t = new Throwable(originT); 
        t.initCause(null); // State Refinement Error
        t.getCause();
    }

}