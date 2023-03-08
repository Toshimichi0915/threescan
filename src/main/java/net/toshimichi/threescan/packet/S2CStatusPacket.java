package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
public class S2CStatusPacket implements S2CPacket {

    private String message;

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        message = Protocol.getString(buffer);
    }
}
