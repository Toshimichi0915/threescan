package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
public class S2CLoginDisconnectPacket implements S2CPacket {

    private String reason;

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        reason = Protocol.getString(buffer);
    }
}
