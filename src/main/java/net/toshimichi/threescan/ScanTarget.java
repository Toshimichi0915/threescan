package net.toshimichi.threescan;

import lombok.Data;

@Data
public class ScanTarget {

    private final String host;
    private final int port;
}
