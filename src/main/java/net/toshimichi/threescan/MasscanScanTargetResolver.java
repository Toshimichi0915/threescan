package net.toshimichi.threescan;

import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;

@RequiredArgsConstructor
public class MasscanScanTargetResolver implements ScanTargetResolver {

    private final BufferedReader reader;

    @Override
    public ScanTarget next() throws IOException {
        String line;
        do {
            line = reader.readLine();
            if (line == null) return null;
        } while (line.startsWith("#"));

        String[] split = line.split(" ");
        return new ScanTarget(split[3], Integer.parseInt(split[2]));
    }
}
