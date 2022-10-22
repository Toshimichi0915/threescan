package net.toshimichi.threescan;

import lombok.Data;

@Data
public class SingleScanTargetResolver implements ScanTargetResolver {

    private final String host;
    private final int portStart;
    private final int portEnd;
    private int index = -1;

    @Override
    public ScanTarget next() {
        if (index == -1) index = portStart;
        if (index > portEnd) return null;
        return new ScanTarget(host, index++);
    }
}
