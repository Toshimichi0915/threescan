package net.toshimichi.threescan;

import lombok.Data;
import net.toshimichi.threescan.scanner.ServerType;

import java.util.List;

@Data
public class ScanResult {

    private String version;
    private int protocol;
    private int playerCount;
    private int maxPlayerCount;
    private List<String> onlinePlayers;
    private List<String> mods;
    private String description;
    private ServerType serverType = ServerType.UNKNOWN;
}
