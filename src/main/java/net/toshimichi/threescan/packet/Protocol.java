package net.toshimichi.threescan.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Protocol {

    private static final int SEGMENT_BITS = 0x7F; // 0111 1111
    private static final int CONTINUE_BIT = 0x80; // 1000 0000

    public static int getVarInt(ByteBuffer buf) throws IOException {
        int value = 0;
        for (int i = 0; i < 5; i++) {
            byte currentByte = buf.get();
            value |= (currentByte & SEGMENT_BITS) << (i * 7);
            if ((currentByte & CONTINUE_BIT) == 0) break;
        }
        return value;
    }

    public static void putVarInt(ByteBuffer buf, int value) throws IOException {
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                buf.put((byte) value);
                return;
            }

            buf.put((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));
            value >>>= 7;
        }
    }

    public static void putFixedVarInt(ByteBuffer buf, int value) throws IOException {
        for (int i = 0; i < 5; i++) {
            if (i == 1) {
                buf.put((byte) (value & SEGMENT_BITS));
                return;
            }
            buf.put((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));
            value >>>= 7;
        }
    }

    public static String getString(ByteBuffer buf) throws IOException {
        byte[] bytes = new byte[getVarInt(buf)];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void putString(ByteBuffer buf, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        putVarInt(buf, bytes.length);
        buf.put(bytes);
    }

    public static boolean getBoolean(ByteBuffer buf) throws IOException {
        return buf.get() == 1;
    }

    public static void putBoolean(ByteBuffer buf, boolean value) throws IOException {
        buf.put(value ? (byte) 1 : 0);
    }

    public static int getUnsignedShort(ByteBuffer buf) throws IOException {
        // convert unsigned short to int
        return buf.getShort() & 0xFFFF;
    }

    public static void putUnsignedShort(ByteBuffer buf, int value) throws IOException {
        buf.putShort((short) (value & 0xFFFF));
    }

    public static UUID getUUID(ByteBuffer buf) throws IOException {
        return new UUID(buf.getLong(), buf.getLong());
    }

    public static void putUUID(ByteBuffer buf, UUID value) throws IOException {
        buf.putLong(value.getMostSignificantBits());
        buf.putLong(value.getLeastSignificantBits());
    }

    public static ByteBuffer getBytes(ByteBuffer buf) throws IOException {
        int length = getVarInt(buf);
        int position = buf.position();
        ByteBuffer slice = buf.slice(position, position + length);
        buf.position(position + length);

        return slice;
    }

    public static void putBytes(ByteBuffer buf, byte[] bytes) throws IOException {
        putVarInt(buf, bytes.length);
        buf.put(bytes);
    }
}
