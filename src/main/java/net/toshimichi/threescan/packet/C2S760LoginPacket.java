package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
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
    public void write(PacketOutputStream out) throws IOException {
        out.writeString(name);
        out.writeBoolean(hasSigName);
        if (hasSigName) {
            out.writeLong(timestamp);
            out.writeBytes(publicKey);
            out.writeBytes(signature);
        }

        out.writeBoolean(hasPlayerUniqueId);
        if (hasPlayerUniqueId) {
            out.writeUUID(playerUniqueId);
        }
    }
}
