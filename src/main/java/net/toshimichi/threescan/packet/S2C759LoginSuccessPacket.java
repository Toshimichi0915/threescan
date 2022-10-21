package net.toshimichi.threescan.packet;

import lombok.Data;
import lombok.Getter;

import java.io.IOException;
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
    public void read(PacketInputStream in) throws IOException {
        uniqueId = in.readUUID();
        username = in.readString();
        int size = in.readVarInt();
        properties = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name = in.readString();
            String value = in.readString();
            boolean hasSignature = in.readBoolean();
            String signature;
            if (hasSignature) {
                signature = in.readString();
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
