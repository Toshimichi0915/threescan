package net.toshimichi.threescan.packet;

import lombok.Getter;

import java.io.IOException;

@Getter
public class S2CEncryptionRequestPacket implements S2CPacket {

    private String serverId;
    private byte[] publicKey;
    private byte[] verifyToken;

    @Override
    public int getId() {
        return 0x01;
    }

    @Override
    public void read(PacketInputStream in) throws IOException {
        serverId = in.readString();
        publicKey = in.readBytes();
        verifyToken = in.readBytes();
    }
}
