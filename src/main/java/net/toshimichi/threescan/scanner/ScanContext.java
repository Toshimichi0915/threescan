package net.toshimichi.threescan.scanner;

import lombok.Data;
import net.toshimichi.threescan.ScanResult;
import net.toshimichi.threescan.packet.C2SPacket;
import net.toshimichi.threescan.packet.Protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

@Data
public class ScanContext {

    // connection
    private final Scanner scanner;
    private SelectionKey selectionKey;
    private boolean connected;
    private boolean reused;
    private boolean cancelled;
    private long startMs;

    // buffer
    private final ByteBuffer readBuffer;
    private long readMs;

    private final ByteBuffer writeBuffer;
    private final ByteBuffer tempBuffer;

    // packet data
    private boolean reading;
    private int packetId;
    private int packetSize;
    private int idPosition;
    private int dataPosition;

    // scan data
    private final ScanTarget scanTarget;
    private ScanState scanState = ScanState.STATUS;
    private ScanResult scanResult = new ScanResult();

    public PacketData readPacket() throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        channel.read(readBuffer);
        if (readBuffer.position() == readBuffer.limit()) {
            throw new IOException("Buffer limit reached");
        }

        if (!reading) {
            int position = readBuffer.position();

            readBuffer.position(0);
            packetSize = Protocol.getVarInt(readBuffer);
            idPosition = readBuffer.position();

            packetId = Protocol.getVarInt(readBuffer);
            dataPosition = readBuffer.position();

            readBuffer.position(position);
            reading = true;
        }

        if (readBuffer.position() - idPosition >= packetSize) {
            reading = false;
            return new PacketData(packetId, readBuffer.slice(dataPosition, readBuffer.position()));
        }

        return null;
    }

    public void writePacket(C2SPacket packet) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        Protocol.putVarInt(writeBuffer, packet.getId());
        packet.write(writeBuffer);
        writeBuffer.flip();

        Protocol.putVarInt(tempBuffer, writeBuffer.limit());
        tempBuffer.flip();

        channel.write(tempBuffer);
        channel.write(writeBuffer);

        writeBuffer.clear();
        tempBuffer.clear();
    }

    public void disconnect(boolean reused) throws IOException {
        selectionKey.cancel();
        selectionKey.channel().close();
        setConnected(false);
        setReused(reused);
    }
}
