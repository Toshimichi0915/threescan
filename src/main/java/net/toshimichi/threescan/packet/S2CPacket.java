package net.toshimichi.threescan.packet;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface S2CPacket extends Packet {

    void read(ByteBuffer buffer) throws IOException;
}
