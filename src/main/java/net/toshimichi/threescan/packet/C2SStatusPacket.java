package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
@AllArgsConstructor
public class C2SStatusPacket implements C2SPacket {

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {

    }
}
