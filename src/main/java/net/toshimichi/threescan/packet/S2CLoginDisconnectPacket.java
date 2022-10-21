package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;

@Getter
public class S2CLoginDisconnectPacket implements S2CPacket {

    private String reason;

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void read(PacketInputStream in) throws IOException {
        reason = in.readString();
    }
}
