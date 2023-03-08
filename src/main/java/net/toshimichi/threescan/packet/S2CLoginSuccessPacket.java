package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

@Getter
public class S2CLoginSuccessPacket implements S2CPacket {

    private UUID uniqueId;
    private String username;

    @Override
    public int getId() {
        return 0x02;
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        uniqueId = Protocol.getUUID(buffer);
        username = Protocol.getString(buffer);
    }
}
