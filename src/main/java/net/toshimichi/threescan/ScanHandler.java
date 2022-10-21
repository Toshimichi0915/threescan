package net.toshimichi.threescan;

@FunctionalInterface
public interface ScanHandler {

    void handle(ScanRequest request, int port, ScanResult result);
}
