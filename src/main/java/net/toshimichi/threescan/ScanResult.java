package net.toshimichi.threescan;

import lombok.Data;

import java.util.List;

@Data
public class ScanResult {

    private final String version;
    private final int playerCount;
    private final int maxPlayerCount;
    private final List<String> onlinePlayers;
    private final String motd;
}
