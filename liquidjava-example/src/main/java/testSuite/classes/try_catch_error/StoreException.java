package testSuite.classes.try_catch_error;

// shape of Apache Derby's StandardException: the constructor may already set the cause
class StoreException extends Exception {
    StoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
