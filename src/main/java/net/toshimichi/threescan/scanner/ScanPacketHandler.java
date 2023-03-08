package net.toshimichi.threescan.scanner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.toshimichi.threescan.ScanHandler;
import net.toshimichi.threescan.ScanResult;
import net.toshimichi.threescan.packet.C2S759LoginPacket;
import net.toshimichi.threescan.packet.C2S760LoginPacket;
import net.toshimichi.threescan.packet.C2S761LoginPacket;
import net.toshimichi.threescan.packet.C2SHandshakePacket;
import net.toshimichi.threescan.packet.C2SLoginPacket;
import net.toshimichi.threescan.packet.C2SStatusPacket;
import net.toshimichi.threescan.packet.S2CLoginDisconnectPacket;
import net.toshimichi.threescan.packet.S2CStatusPacket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

@RequiredArgsConstructor
public class ScanPacketHandler implements PacketHandler {

    private final ScanMode scanMode;
    private final ScanHandler scanHandler;

    private final String name;
    private final UUID uniqueId;

    private final Gson gson = new Gson();

    @Override
    public void onConnected(ScanContext context) throws IOException {
        ScanTarget target = context.getScanTarget();
        ScanState state = context.getScanState();
        switch (state) {
            case STATUS -> {
                context.writePacket(new C2SHandshakePacket(760, target.getHost(), target.getPort(), state.getId()));
                context.writePacket(new C2SStatusPacket());
            }
            case LOGIN -> {
                int protocol = context.getScanResult().getProtocol();
                context.writePacket(new C2SHandshakePacket(protocol, target.getHost(), target.getPort(), state.getId()));
                if (protocol >= 761) {
                    context.writePacket(new C2S761LoginPacket(name, uniqueId));
                } else if (protocol == 760) {
                    context.writePacket(new C2S760LoginPacket(name, uniqueId));
                } else if (protocol == 759) {
                    context.writePacket(new C2S759LoginPacket(name));
                } else {
                    context.writePacket(new C2SLoginPacket(name));
                }
            }
            default -> {
                throw new RuntimeException("Unknown state: " + state);
            }
        }
    }

    @Override
    public void onPacketReceived(ScanContext context, PacketData data) throws IOException {
        ScanState state = context.getScanState();
        switch (state) {
            case STATUS -> {
                if (data.getId() != 0) {
                    throw new IOException("Illegal packet ID: " + data.getId());
                }

                S2CStatusPacket packet = new S2CStatusPacket();
                packet.read(data.getBuffer());
                parseStatus(context, packet);
            }
            case LOGIN -> {
                parseLogin(context, data);
            }
            default -> {
                throw new RuntimeException("Unknown state: " + state);
            }
        }
    }

    public String parseComponent(JsonArray array) {
        StringBuilder builder = new StringBuilder();
        for (JsonElement element : array) {
            builder.append(element.getAsJsonObject().get("text").getAsString());
        }
        return builder.toString();
    }

    public void parseStatus(ScanContext context, S2CStatusPacket packet) throws IOException {
        ScanTarget target = context.getScanTarget();
        ScanResult result = context.getScanResult();

        try {
            JsonObject obj = gson.fromJson(packet.getMessage(), JsonObject.class);
            result.setVersion(obj.getAsJsonObject("version").get("name").getAsString());
            result.setProtocol(obj.getAsJsonObject("version").get("protocol").getAsInt());
            result.setPlayerCount(obj.getAsJsonObject("players").get("online").getAsInt());
            result.setMaxPlayerCount(obj.getAsJsonObject("players").get("max").getAsInt());

            result.setOnlinePlayers(new ArrayList<>());
            JsonArray sample = obj.getAsJsonObject("players").getAsJsonArray("sample");
            if (sample != null) {
                for (JsonElement element : sample) {
                    result.getOnlinePlayers().add(element.getAsJsonObject().get("name").getAsString());
                }
            }

            JsonElement description = obj.get("description");
            if (description.isJsonObject()) {
                JsonObject desc = description.getAsJsonObject();
                if (desc.has("extra")) {
                    result.setDescription(parseComponent(desc.getAsJsonArray("extra")));
                } else {
                    result.setDescription(desc.get("text").getAsString());
                }
            } else {
                result.setDescription(description.getAsString());
            }
        } catch (Exception e) {
            throw new InvalidStatusException("Failed to parse server status: " + e.getMessage() + " while scanning " + target.getHost() + ":" + target.getPort());
        }

        context.disconnect(true);
        switch (scanMode) {
            case FAST -> {
                scanHandler.handle(target, result);
            }
            case FULL -> {
                // re-scan for full data
                context.setScanState(ScanState.LOGIN);
                context.getScanner().scan(context);
            }
            default -> {
                throw new RuntimeException("Unknown mode: " + scanMode);
            }
        }
    }

    public void parseLogin(ScanContext context, PacketData data) throws IOException {
        ScanTarget target = context.getScanTarget();
        ScanResult result = context.getScanResult();
        switch (data.getId()) {
            case 0 -> {
                S2CLoginDisconnectPacket packet = new S2CLoginDisconnectPacket();
                packet.read(data.getBuffer());
                if (packet.getReason().contains("IP forwarding")) {
                    result.setServerType(ServerType.BUNGEECORD);
                } else if (packet.getReason().contains("Forge")) {
                    result.setServerType(ServerType.MODDED);
                } else if (packet.getReason().contains("whitelisted")) {
                    result.setServerType(ServerType.WHITELISTED);
                } else if (packet.getReason().contains("Unable to authenticate")) {
                    result.setServerType(ServerType.VELOCITY);
                } else {
                    System.err.println("Could not determine server type: " + packet.getReason() + " while scanning " + target.getHost() + ":" + target.getPort());
                    result.setServerType(ServerType.UNKNOWN);
                }
            }
            case 1 -> {
                // S2CEncryptionRequestPacket
                result.setServerType(ServerType.ONLINE);
            }
            case 2, 3 -> {
                // S2CLoginSuccessPacket, S2CCompressionPacket
                result.setServerType(ServerType.OFFLINE);
            }
            default -> {
                throw new IOException("Illegal packet ID: " + data.getId());
            }
        }

        context.disconnect(false);
        scanHandler.handle(target, result);
    }
}
