package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
public class S2CEncryptionRequestPacket implements S2CPacket {

    private String serverId;
    private ByteBuffer publicKey;
    private ByteBuffer verifyToken;

    @Override
    public int getId() {
        return 0x01;
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        serverId = Protocol.getString(buffer);
        publicKey = Protocol.getBytes(buffer);
        verifyToken = Protocol.getBytes(buffer);
    }
}
