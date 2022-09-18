package xyz.synse.udppacketer;

public class PacketerException extends RuntimeException {

    public PacketerException(String message, Throwable cause){
        super(message, cause);
    }

    public PacketerException(String message){
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    public synchronized Throwable getCause() {
        return super.getCause();
    }
}
