package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
@AllArgsConstructor
public class C2SHandshakePacket implements C2SPacket {

    private int version;
    private String host;
    private int port;
    private int nextState;

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        Protocol.putVarInt(buffer, version);
        Protocol.putString(buffer, host);
        Protocol.putUnsignedShort(buffer, port);
        Protocol.putVarInt(buffer, nextState);
    }
}
