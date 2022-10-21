package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;

@Getter
@AllArgsConstructor
public class C2SHandshakePacket implements C2SPacket {

    private int version;
    private String host;
    private int port;
    private int nextState;

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void write(PacketOutputStream out) throws IOException {
        out.writeVarInt(version);
        out.writeString(host);
        out.writeUnsignedShort(port);
        out.writeVarInt(nextState);
    }
}
