package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;

@Getter
public class S2CStatusPacket implements S2CPacket {

    private String message;

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void read(PacketInputStream in) throws IOException {
        message = in.readString();
    }
}
