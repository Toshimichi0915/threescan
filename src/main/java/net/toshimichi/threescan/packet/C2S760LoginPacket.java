package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class C2S760LoginPacket implements C2SPacket {

    private String name;
    private boolean hasSigName;
    private long timestamp;
    private byte[] publicKey;
    private byte[] signature;
    private boolean hasPlayerUniqueId;
    private UUID playerUniqueId;

    public C2S760LoginPacket(String name, UUID uuid) {
        this(name, false, 0, new byte[0], new byte[0], true, uuid);
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

        Protocol.putBoolean(buffer, hasPlayerUniqueId);
        if (hasPlayerUniqueId) {
            Protocol.putUUID(buffer, playerUniqueId);
        }
    }
}
