package net.toshimichi.threescan.scanner;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
public class RateLimitScanner implements Scanner, Runnable {

    private final double scanPerMs;
    private final Scanner scanner;

    private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(10_000);

    private Thread thread;
    private double limit;
    private long lastMs;
    private volatile boolean stopped;

    @SneakyThrows
    @Override
    public void scan(ScanTarget target) {
        queue.put(target);
    }

    @SneakyThrows
    @Override
    public void scan(ScanContext context) {
        queue.put(context);
    }

    @Override
    public void start() {
        scanner.start();

        thread = new Thread(this);
        thread.setName("rate-limit-" + thread.getId());
        thread.start();
        lastMs = System.currentTimeMillis();
    }

    @Override
    public void stop() throws InterruptedException {
        stopped = true;
        thread.join();

        scanner.stop();
    }

    @Override
    public void run() {
        while (!stopped || !queue.isEmpty()) {
            long currentMs = System.currentTimeMillis();
            if (queue.isEmpty()) {
                lastMs = currentMs;

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
                continue;
            }

            limit += scanPerMs * (currentMs - lastMs);
            lastMs = currentMs;

            while (limit > 0) {
                Object o = queue.poll();
                if (o == null) {
                    break;
                } else if (o instanceof ScanTarget target) {
                    scanner.scan(target);
                } else if (o instanceof ScanContext context) {
                    scanner.scan(context);
                }
                limit--;
            }
        }
    }
}
