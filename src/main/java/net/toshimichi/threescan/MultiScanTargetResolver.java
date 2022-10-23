package net.toshimichi.threescan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MultiScanTargetResolver implements ScanTargetResolver {

    private final List<String> targets;
    private final int portStart;
    private final int portEnd;
    private int index;

    public MultiScanTargetResolver(Path path, int portStart, int portEnd) throws IOException {
        targets = Files.readAllLines(path);
        this.portStart = portStart;
        this.portEnd = portEnd;
    }

    @Override
    public ScanTarget next() {
        if (index >= targets.size()) return null;
        int port = portStart + index % (portEnd - portStart + 1);
        String target = targets.get(index / (portEnd - portStart + 1));
        index++;
        return new ScanTarget(target, port);
    }
}
