package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;

@Getter
public class S2CCompressionPacket implements S2CPacket {

    private int threshold;

    @Override
    public int getId() {
        return 0x03;
    }

    @Override
    public void read(PacketInputStream in) throws IOException {
        threshold = in.readVarInt();
    }
}
