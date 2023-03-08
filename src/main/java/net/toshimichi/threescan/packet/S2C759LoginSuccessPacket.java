package net.toshimichi.threescan.packet;

import lombok.Data;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

@Getter
public class S2C759LoginSuccessPacket implements S2CPacket {

    private UUID uniqueId;
    private String username;
    private ArrayList<LoginProperty> properties;

    @Override
    public int getId() {
        return 0x02;
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        uniqueId = Protocol.getUUID(buffer);
        username = Protocol.getString(buffer);
        int size = Protocol.getVarInt(buffer);
        properties = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name = Protocol.getString(buffer);
            String value = Protocol.getString(buffer);
            boolean hasSignature = Protocol.getBoolean(buffer);
            String signature;
            if (hasSignature) {
                signature = Protocol.getString(buffer);
            } else {
                signature = null;
            }
            properties.add(new LoginProperty(name, value, hasSignature, signature));
        }
    }

    @Data
    public static class LoginProperty {

        private final String name;
        private final String value;
        private final boolean hasSignature;
        private final String signature;
    }
}
