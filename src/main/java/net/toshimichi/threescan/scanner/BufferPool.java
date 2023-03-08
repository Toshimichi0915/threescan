package net.toshimichi.threescan.scanner;

import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;

@RequiredArgsConstructor
public class BufferPool {

    private final ArrayDeque<ByteBuffer> buffers = new ArrayDeque<>();
    private final int bufferSize;

    public ByteBuffer get() {
        ByteBuffer buffer = buffers.poll();
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(bufferSize);
        }
        return buffer;
    }

    public void release(ByteBuffer buffer) {
        buffer.clear();
        buffers.add(buffer);
    }
}
