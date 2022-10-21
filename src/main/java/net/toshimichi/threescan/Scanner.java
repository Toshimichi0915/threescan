package net.toshimichi.threescan;

public interface Scanner {

    void scan(ScanRequest request, ScanHandler handler);

    void shutdown();
}
