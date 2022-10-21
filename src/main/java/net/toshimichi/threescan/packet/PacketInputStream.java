package net.toshimichi.threescan.packet;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
public class PacketInputStream {

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    @Delegate
    private final InputStream in;

    public int readVarInt() throws IOException {
        int value = 0;
        int position = 0;
        int currentByte;

        while (true) {
            currentByte = read();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new IOException("VarInt is too big");
        }

        return value;
    }

    public String readString() throws IOException {
        byte[] bytes = new byte[readVarInt()];
        read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public boolean readBoolean() throws IOException {
        return read() == 1;
    }

    public long readLong() throws IOException {
        return ((long) read() << 56) |
                ((long) read() << 48) |
                ((long) read() << 40) |
                ((long) read() << 32) |
                ((long) read() << 24) |
                ((long) read() << 16) |
                ((long) read() << 8) |
                read();
    }

    public int readUnsignedShort() throws IOException {
        return (read() << 8) | read();
    }

    public UUID readUUID() throws IOException {
        return new UUID(readLong(), readLong());
    }

    public byte[] readBytes() throws IOException {
        byte[] bytes = new byte[readVarInt()];
        read(bytes);
        return bytes;
    }

    public <T extends S2CPacket> void readPacket(T packet) throws IOException {
        int len = readVarInt();

        ByteArrayInputStream buff = new ByteArrayInputStream(readNBytes(len));
        PacketInputStream in = new PacketInputStream(buff);

        int id = in.readVarInt();
        if (packet.getId() != id) {
            throw new IOException("Packet ID does not match. Expected: " + packet.getId() + " Actual: " + id);
        }

        packet.read(in);
    }

    public S2CPacket readPacket(Function<Integer, S2CPacket> function) throws IOException {
        int len = readVarInt();

        ByteArrayInputStream buff = new ByteArrayInputStream(readNBytes(len));
        PacketInputStream in = new PacketInputStream(buff);

        int id = in.readVarInt();
        S2CPacket packet = function.apply(id);
        if (packet == null) {
            throw new IOException("Unknown packet ID: " + id);
        }

        packet.read(in);
        return packet;
    }
}
