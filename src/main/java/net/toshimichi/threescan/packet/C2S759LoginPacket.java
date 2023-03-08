package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
@AllArgsConstructor
public class C2S759LoginPacket implements C2SPacket {

    private String name;
    private boolean hasSigName;
    private long timestamp;
    private byte[] publicKey;
    private byte[] signature;

    public C2S759LoginPacket(String name) {
        this(name, false, 0, new byte[0], new byte[0]);
    }

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        Protocol.putString(buffer, name);
        Protocol.putBoolean(buffer, hasSigName);
        if (hasSigName) {
            buffer.putLong(timestamp);
            Protocol.putBytes(buffer, publicKey);
            Protocol.putBytes(buffer, signature);
        }
    }
}
