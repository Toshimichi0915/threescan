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
import net.toshimichi.threescan.packet.Protocol;
import net.toshimichi.threescan.packet.S2CLoginDisconnectPacket;
import net.toshimichi.threescan.packet.S2CLoginPluginRequestPacket;
import net.toshimichi.threescan.packet.S2CStatusPacket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
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

    // https://github.com/MinecraftForge/MinecraftForge/blob/f65381fbffd10285a05a46ee101fe75256de542e/src/main/java/net/minecraftforge/network/ServerStatusPing.java#L307
    private static ByteBuffer decodeForgeMessage(String s) {
        var size0 = ((int) s.charAt(0));
        var size1 = ((int) s.charAt(1));
        var size = size0 | (size1 << 15);

        var buf = ByteBuffer.allocate(size);

        int stringIndex = 2;
        int buffer = 0; // we will need at most 8 + 14 = 22 bits of buffer, so an int is enough
        int bitsInBuf = 0;
        while (stringIndex < s.length()) {
            while (bitsInBuf >= 8) {
                buf.put((byte) buffer);
                buffer >>>= 8;
                bitsInBuf -= 8;
            }

            var c = s.charAt(stringIndex);
            buffer |= (((int) c) & 0x7FFF) << bitsInBuf;
            bitsInBuf += 15;
            stringIndex++;
        }

        // write any leftovers
        while (buf.remaining() > 0) {
            buf.put((byte) buffer);
            buffer >>>= 8;
            bitsInBuf -= 8;
        }

        return buf;
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

            JsonObject forgeData = obj.getAsJsonObject("forgeData");
            List<String> mods = new ArrayList<>();
            if (forgeData != null) {
                JsonElement compact = forgeData.get("d");
                JsonElement legacy = forgeData.get("mods");
                if (compact != null) {
                    String d = forgeData.get("d").getAsString();
                    ByteBuffer buf = decodeForgeMessage(d);
                    buf.flip();

                    Protocol.getBoolean(buf); // truncated
                    int modCount = Protocol.getUnsignedShort(buf);

                    for (int i = 0; i < modCount; i++) {
                        int flags = Protocol.getVarInt(buf); // channel size & version flag
                        int channelCount = flags >>> 1;
                        boolean serverOnly = (flags & 0b1) != 0;
                        String modId = Protocol.getString(buf);
                        String modVersion = serverOnly ? "UNKNOWN" : Protocol.getString(buf);
                        for (int j = 0; j < channelCount; j++) {
                            Protocol.getString(buf); // channel name
                            Protocol.getString(buf); // channel version
                            Protocol.getBoolean(buf); // required on client
                        }
                        mods.add(modId + " " + modVersion);
                    }
                } else if (legacy != null) {
                    for (JsonElement element : legacy.getAsJsonArray()) {
                        JsonObject mod = element.getAsJsonObject();
                        mods.add(mod.get("modId").getAsString() + " " + mod.get("modmarker").getAsString());
                    }
                }
            }
            result.setMods(mods);

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
            e.printStackTrace();
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
            case 0x00 -> {
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
            case 0x01 -> {
                // S2CEncryptionRequestPacket
                result.setServerType(ServerType.ONLINE);
            }
            case 0x02, 0x03 -> {
                // S2CLoginSuccessPacket, S2CCompressionPacket
                result.setServerType(ServerType.OFFLINE);
            }
            case 0x04 -> {
                S2CLoginPluginRequestPacket packet = new S2CLoginPluginRequestPacket();
                packet.read(data.getBuffer());
                if (packet.getChannel().equals("velocity:player_info")) {
                    result.setServerType(ServerType.VELOCITY);
                } else {
                    result.setServerType(ServerType.CUSTOM);
                    System.err.println("Found unknown login plugin request: " + packet.getChannel() + " while scanning " + target.getHost() + ":" + target.getPort());
                }
            }
            default -> {
                throw new IOException("Illegal packet ID: " + data.getId());
            }
        }

        context.disconnect(false);
        scanHandler.handle(target, result);
    }
}
