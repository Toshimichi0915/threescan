package net.toshimichi.threescan;

public interface Scanner {

    void scan(ScanTargetResolver resolver, ScanHandler handler);

    void shutdown();
}
