package net.toshimichi.threescan.scanner;

import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;

@RequiredArgsConstructor
public class RateLimitScanner implements Scanner, Runnable {

    private final double scanPerMs;
    private final Scanner scanner;

    private final ArrayDeque<Object> queue = new ArrayDeque<>();

    private Thread thread;
    private double limit;
    private long lastMs;
    private boolean stopped;

    @Override
    public void scan(ScanTarget target) {
        synchronized (queue) {
            queue.add(target);
        }
    }

    @Override
    public void scan(ScanContext context) {
        synchronized (queue) {
            queue.add(context);
        }
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
        while (!stopped || queue.size() > 0) {
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
                Object o;
                synchronized (queue) {
                    o = queue.poll();
                }
                if (o instanceof ScanTarget) {
                    scanner.scan((ScanTarget) o);
                } else if (o instanceof ScanContext) {
                    scanner.scan((ScanContext) o);
                }
                limit--;
            }
        }
    }
}
