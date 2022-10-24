package net.toshimichi.threescan;

import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;

@RequiredArgsConstructor
public class RangeScanTargetResolver implements ScanTargetResolver {

    private final BufferedReader reader;
    private int fromIpv4;
    private int toIpv4;
    private int portStart;
    private int portEnd;
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

    @Override
    public ScanTarget next() throws IOException {
        if (index == 0 || index >= (toIpv4 - fromIpv4 + 1) * (portEnd - portStart + 1)) {
            String line = reader.readLine();
            if (line == null) return null;
            String[] split = line.split(" ");

            index = 0;
            portStart = Integer.parseInt(split[1]);
            portEnd = Integer.parseInt(split[2]);

            if (split[0].contains("/")) {
                String[] split2 = split[0].split("/");
                int prefix = Integer.parseInt(split2[1]);
                fromIpv4 = ipv4ToInt(split2[0]) & (0xffffffff << (32 - prefix));
                toIpv4 = fromIpv4 + (1 << (32 - prefix)) - 1;
            } else if (split[1].contains("-")) {
                String[] split2 = split[0].split("-");
                fromIpv4 = ipv4ToInt(split2[0]);
                toIpv4 = ipv4ToInt(split2[1]);
            } else {
                fromIpv4 = ipv4ToInt(split[0]);
                toIpv4 = fromIpv4;
            }
        }

        int port = portStart + index % (portEnd - portStart + 1);
        int ipv4 = fromIpv4 + index / (portEnd - portStart + 1);
        index++;

        return new ScanTarget(ipv4ToString(ipv4), port);
    }
}
