package net.toshimichi.threescan.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;

@Getter
@AllArgsConstructor
public class C2S759LoginPacket implements C2SPacket {

    private String name;
    private boolean hasSigName;
    private long timestamp;
    private byte[] publicKey;
    private byte[] signature;

    public C2S759LoginPacket(String name) {
        this(name, false, 0, new byte[0], new byte[0]);
    }

    @Override
    public int getId() {
        return 0x00;
    }

    @Override
    public void write(PacketOutputStream out) throws IOException {
        out.writeString(name);
        out.writeBoolean(hasSigName);
        if (hasSigName) {
            out.writeLong(timestamp);
            out.writeBytes(publicKey);
            out.writeBytes(signature);
        }
    }
}
