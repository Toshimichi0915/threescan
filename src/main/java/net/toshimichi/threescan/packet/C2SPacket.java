package net.toshimichi.threescan.packet;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface C2SPacket extends Packet {

    void write(ByteBuffer buffer) throws IOException;
}
