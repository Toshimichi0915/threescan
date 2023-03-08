package net.toshimichi.threescan;

import net.toshimichi.threescan.scanner.ScanTarget;

@FunctionalInterface
public interface ScanHandler {

    void handle(ScanTarget target, ScanResult result);
}
