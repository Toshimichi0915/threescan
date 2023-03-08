package net.toshimichi.threescan.resolver;

import lombok.RequiredArgsConstructor;
import net.toshimichi.threescan.scanner.ScanTarget;

import java.io.BufferedReader;
import java.io.IOException;

@RequiredArgsConstructor
public class MasscanScanTargetResolver implements ScanTargetResolver {

    private final BufferedReader reader;

    @Override
    public ScanTarget next() throws IOException {
        String line = reader.readLine();
        if (line == null) return null;
        String[] split = line.split(" ");
        return new ScanTarget(split[5], Integer.parseInt(split[3].replaceAll("/.*", "")));
    }
}
