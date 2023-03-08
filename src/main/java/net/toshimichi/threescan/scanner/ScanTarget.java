package net.toshimichi.threescan.scanner;

import lombok.Data;

@Data
public class ScanTarget {

    private final String host;
    private final int port;
}
