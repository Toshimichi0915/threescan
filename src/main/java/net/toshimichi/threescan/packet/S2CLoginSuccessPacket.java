package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;
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
    public void read(PacketInputStream in) throws IOException {
        uniqueId = in.readUUID();
        username = in.readString();
    }
}
