package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class C2S761LoginPacket implements C2SPacket {

    private String name;
    private boolean hasPlayerUniqueId;
    private UUID playerUniqueId;

    public C2S761LoginPacket(String name, UUID uuid) {
        this(name, true, uuid);
    }

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        Protocol.putString(buffer, name);

        Protocol.putBoolean(buffer, hasPlayerUniqueId);
        if (hasPlayerUniqueId) {
            Protocol.putUUID(buffer, playerUniqueId);
        }
    }
}
