package net.toshimichi.threescan;

@FunctionalInterface
public interface ScanHandler {

    void handle(String host, int port, ScanResult result);
}
