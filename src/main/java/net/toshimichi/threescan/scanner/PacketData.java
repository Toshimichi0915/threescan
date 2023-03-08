package net.toshimichi.threescan.scanner;

import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class PacketData {

    private final int id;
    private final ByteBuffer buffer;
}
