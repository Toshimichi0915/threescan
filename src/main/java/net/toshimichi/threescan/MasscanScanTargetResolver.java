package net.toshimichi.threescan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MasscanScanTargetResolver implements ScanTargetResolver {

    private final List<ScanTarget> targets;
    private int index;

    public MasscanScanTargetResolver(Path path) throws IOException {
        targets = Files.readAllLines(path).stream()
                .filter(it -> !it.startsWith("#"))
                .map(it -> it.split(" "))
                .map(it -> new ScanTarget(it[3], Integer.parseInt(it[2])))
                .toList();
    }

    @Override
    public ScanTarget next() {
        if (index >= targets.size()) return null;
        return targets.get(index++);
    }
}
