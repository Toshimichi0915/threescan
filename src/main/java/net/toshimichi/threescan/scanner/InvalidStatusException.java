package net.toshimichi.threescan.scanner;

import java.io.IOException;

public class InvalidStatusException extends IOException {

    public InvalidStatusException() {
    }

    public InvalidStatusException(String message) {
        super(message);
    }

    public InvalidStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidStatusException(Throwable cause) {
        super(cause);
    }
}
