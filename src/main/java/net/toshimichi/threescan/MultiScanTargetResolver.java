package net.toshimichi.threescan;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MultiScanTargetResolver implements ScanTargetResolver {

    private final int fromIpv4;
    private final int toIpv4;
    private final int portStart;
    private final int portEnd;
    private int index;

    private int ipv4ToInt(String str) {
        String[] split = str.split("\\.");
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            ret += Integer.parseInt(split[i]) << (8 * (3 - i));
        }
        return ret;
    }

    private String ipv4ToString(int ipv4) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append((ipv4 >> (8 * (3 - i))) & 0xff);
            if (i != 3) sb.append('.');
        }
        return sb.toString();
    }

    public MultiScanTargetResolver(String fromIpv4, String toIpv4, int portStart, int portEnd) {
        this.fromIpv4 = ipv4ToInt(fromIpv4);
        this.toIpv4 = ipv4ToInt(toIpv4);
        this.portStart = portStart;
        this.portEnd = portEnd;
    }

    public MultiScanTargetResolver(String ipv4, int prefix, int portStart, int portEnd) {
        this.fromIpv4 = ipv4ToInt(ipv4) & (0xffffffff << (32 - prefix));
        this.toIpv4 = fromIpv4 + (1 << (32 - prefix)) - 1;
        this.portStart = portStart;
        this.portEnd = portEnd;
    }

    @Override
    public ScanTarget next() {
        int port = portStart + index % (portEnd - portStart + 1);
        int ipv4 = fromIpv4 + index / (portEnd - portStart + 1);
        index++;

        if (ipv4 > toIpv4) return null;
        return new ScanTarget(ipv4ToString(ipv4), port);
    }
}
