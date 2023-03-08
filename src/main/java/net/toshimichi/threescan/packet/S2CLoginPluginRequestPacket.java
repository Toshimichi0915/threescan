package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
public class S2CLoginPluginRequestPacket implements S2CPacket {

    private int messageId;
    private String channel;
    private ByteBuffer data;

    @Override
    public int getId() {
        return 0x04;
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        messageId = Protocol.getVarInt(buffer);
        channel = Protocol.getString(buffer);
        data = buffer.slice(buffer.position(), buffer.remaining());
    }
}
