package net.toshimichi.threescan.packet;

import java.io.IOException;

public interface S2CPacket extends Packet {

    void read(PacketInputStream in) throws IOException;
}
