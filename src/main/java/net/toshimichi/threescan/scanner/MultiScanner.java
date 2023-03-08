package net.toshimichi.threescan.scanner;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class MultiScanner implements Scanner {

    private final Supplier<Scanner> supplier;

    private Scanner[] scanners;
    private int counter;

    public void start() {
        int threadCount = Runtime.getRuntime().availableProcessors();

        scanners = new Scanner[threadCount];
        for (int i = 0; i < threadCount; i++) {
            scanners[i] = supplier.get();
            scanners[i].start();
        }
    }

    @Override
    public void stop() throws InterruptedException {
        for (Scanner scanner : scanners) {
            scanner.stop();
        }
    }

    @Override
    public void scan(ScanTarget target) {
        scanners[++counter % scanners.length].scan(target);
    }

    @Override
    public void scan(ScanContext context) {
        scanners[++counter % scanners.length].scan(context);
    }
}
