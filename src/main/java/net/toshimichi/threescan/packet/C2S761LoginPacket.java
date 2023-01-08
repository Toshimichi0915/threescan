package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
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
    public void write(PacketOutputStream out) throws IOException {
        out.writeString(name);

        out.writeBoolean(hasPlayerUniqueId);
        if (hasPlayerUniqueId) {
            out.writeUUID(playerUniqueId);
        }
    }
}
