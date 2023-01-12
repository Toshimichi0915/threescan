package net.toshimichi.threescan;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;

@RequiredArgsConstructor
public class ThreescanScanTargetResolver implements ScanTargetResolver {

    private final BufferedReader reader;
    private final Gson gson = new Gson();

    @Override
    public ScanTarget next() throws IOException {
        String line = reader.readLine();
        if (line == null) return null;

        return gson.fromJson(line, ScanTarget.class);
    }
}
