package net.toshimichi.threescan.packet;

import java.io.IOException;

public interface C2SPacket extends Packet {

    void write(PacketOutputStream out) throws IOException;
}
