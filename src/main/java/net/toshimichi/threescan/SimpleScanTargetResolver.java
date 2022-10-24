package net.toshimichi.threescan;

import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;

@Data
public class SimpleScanTargetResolver implements ScanTargetResolver {

    private final BufferedReader reader;
    private String host;
    private int portStart;
    private int portEnd;
    private int index;

    @Override
    public ScanTarget next() throws IOException {
        if (index == 0 || index >= portEnd - portStart + 1) {
            String line = reader.readLine();
            if (line == null) return null;
            String[] split = line.split(" ");

            index = 0;
            host = split[0];
            portStart = Integer.parseInt(split[1]);
            portEnd = Integer.parseInt(split[2]);
        }

        return new ScanTarget(host, portStart + index++);
    }
}
