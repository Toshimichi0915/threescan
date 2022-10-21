package net.toshimichi.threescan.packet;

import java.io.IOException;

public class C2SStatusPacket implements C2SPacket {

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void write(PacketOutputStream out) throws IOException {

    }
}
