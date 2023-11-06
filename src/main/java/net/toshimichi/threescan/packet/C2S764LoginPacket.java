package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class C2S764LoginPacket implements C2SPacket {

    private String name;
    private UUID playerUniqueId;

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        Protocol.putString(buffer, name);
        Protocol.putUUID(buffer, playerUniqueId);
    }
}
