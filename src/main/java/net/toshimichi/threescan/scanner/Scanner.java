package net.toshimichi.threescan.scanner;

public interface Scanner {

    void scan(ScanTarget target);

    void scan(ScanContext context);

    void start();

    void stop() throws InterruptedException;
}
