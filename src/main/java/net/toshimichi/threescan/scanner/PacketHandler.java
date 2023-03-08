package net.toshimichi.threescan.scanner;

import java.io.IOException;

public interface PacketHandler {

    void onConnected(ScanContext context) throws IOException;

    void onPacketReceived(ScanContext context, PacketData packet) throws IOException;
}
