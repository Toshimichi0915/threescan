package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
public class S2CCompressionPacket implements S2CPacket {

    private int threshold;

    @Override
    public int getId() {
        return 0x03;
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        threshold = Protocol.getVarInt(buffer);
    }
}
