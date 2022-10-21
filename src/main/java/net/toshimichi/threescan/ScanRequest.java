package net.toshimichi.threescan;

import lombok.Data;

@Data
public class ScanRequest {

    private final String host;
    private final int portStart;
    private final int portEnd;
}
