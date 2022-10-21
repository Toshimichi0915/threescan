package net.toshimichi.threescan.packet;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class PacketOutputStream extends OutputStream {

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    @Delegate
    private final OutputStream out;

    public void writeVarInt(int value) throws IOException {
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                write(value);
                return;
            }

            write((value & SEGMENT_BITS) | CONTINUE_BIT);

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
        }
    }

    public void writeString(String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        write(bytes);
    }

    public void writeUnsignedShort(int value) throws IOException {
        write(value >> 8);
        write(value);
    }

    public void writePacket(C2SPacket packet) throws IOException {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        PacketOutputStream out = new PacketOutputStream(buff);
        out.writeVarInt(packet.getId());
        packet.write(out);

        writeVarInt(buff.size());
        write(buff.toByteArray());
    }
}
