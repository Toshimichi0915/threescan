package net.toshimichi.threescan;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.toshimichi.threescan.packet.C2S759LoginPacket;
import net.toshimichi.threescan.packet.C2S760LoginPacket;
import net.toshimichi.threescan.packet.C2SHandshakePacket;
import net.toshimichi.threescan.packet.C2SLoginPacket;
import net.toshimichi.threescan.packet.C2SStatusPacket;
import net.toshimichi.threescan.packet.PacketInputStream;
import net.toshimichi.threescan.packet.PacketOutputStream;
import net.toshimichi.threescan.packet.S2C759LoginSuccessPacket;
import net.toshimichi.threescan.packet.S2CCompressionPacket;
import net.toshimichi.threescan.packet.S2CEncryptionRequestPacket;
import net.toshimichi.threescan.packet.S2CLoginDisconnectPacket;
import net.toshimichi.threescan.packet.S2CLoginSuccessPacket;
import net.toshimichi.threescan.packet.S2CPacket;
import net.toshimichi.threescan.packet.S2CStatusPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@RequiredArgsConstructor
public class ExecutorScanner implements Scanner {

    private static final String NAME = "Hiyokomame0144";
    private static final UUID UNIQUE_ID = UUID.fromString("1bd678d3-037c-4c21-9865-8d60ad282c57");
    private static final Gson gson = new Gson();
    private final ThreadPoolExecutor executor;
    private final int queueSize;
    private final int timeout;
    private final boolean serverCheck;

    public String parseComponent(JsonArray array) {
        StringBuilder builder = new StringBuilder();
        for (JsonElement element : array) {
            builder.append(element.getAsJsonObject().get("text").getAsString());
        }
        return builder.toString();
    }

    public ScanResult scan(String host, int port) throws IOException {

        String version;
        int protocol;
        int playerCount;
        int maxPlayerCount;
        ArrayList<String> onlinePlayers;
        String motd;
        ServerType serverType;

        try (Socket socket = new Socket()) {
            socket.setSoTimeout(timeout);
            socket.connect(new InetSocketAddress(host, port), timeout);

            PacketInputStream in = new PacketInputStream(socket.getInputStream());
            PacketOutputStream out = new PacketOutputStream(socket.getOutputStream());

            // handshake
            out.writePacket(new C2SHandshakePacket(760, host, port, 1));
            out.writePacket(new C2SStatusPacket());

            S2CStatusPacket statusPacket = new S2CStatusPacket();
            in.readPacket(statusPacket);

            try {
                JsonObject obj = gson.fromJson(statusPacket.getMessage(), JsonObject.class);
                version = obj.getAsJsonObject("version").get("name").getAsString();
                protocol = obj.getAsJsonObject("version").get("protocol").getAsInt();
                playerCount = obj.getAsJsonObject("players").get("online").getAsInt();
                maxPlayerCount = obj.getAsJsonObject("players").get("max").getAsInt();
                onlinePlayers = new ArrayList<>();
                JsonArray sample = obj.getAsJsonObject("players").getAsJsonArray("sample");
                if (sample != null) {
                    for (JsonElement element : sample) {
                        onlinePlayers.add(element.getAsJsonObject().get("name").getAsString());
                    }
                }

                JsonElement description = obj.get("description");
                if (description.isJsonObject()) {
                    JsonObject desc = description.getAsJsonObject();
                    if (desc.has("extra")) {
                        motd = parseComponent(desc.getAsJsonArray("extra"));
                    } else {
                        motd = desc.get("text").getAsString();
                    }
                } else {
                    motd = description.getAsString();
                }
            } catch (Exception e) {
                throw new InvalidStatusException(statusPacket.getMessage());
            }
        }

        if (serverCheck) {
            try (Socket socket = new Socket()) {
                socket.setSoTimeout(timeout);
                socket.connect(new InetSocketAddress(host, port), timeout);

                PacketInputStream in = new PacketInputStream(socket.getInputStream());
                PacketOutputStream out = new PacketOutputStream(socket.getOutputStream());

                out.writePacket(new C2SHandshakePacket(protocol, host, port, 2));
                Map<Integer, S2CPacket> packets;
                if (protocol >= 759) {
                    if (protocol == 760) {
                        out.writePacket(new C2S760LoginPacket(NAME, UNIQUE_ID));
                    } else {
                        out.writePacket(new C2S759LoginPacket(NAME));
                    }
                    packets = Map.of(
                            0x00, new S2CLoginDisconnectPacket(),
                            0x01, new S2CEncryptionRequestPacket(),
                            0x02, new S2C759LoginSuccessPacket(),
                            0x03, new S2CCompressionPacket()
                    );
                } else {
                    out.writePacket(new C2SLoginPacket(NAME));
                    packets = Map.of(
                            0x00, new S2CLoginDisconnectPacket(),
                            0x01, new S2CEncryptionRequestPacket(),
                            0x02, new S2CLoginSuccessPacket(),
                            0x03, new S2CCompressionPacket()
                    );
                }

                S2CPacket packet = in.readPacket((Function<Integer, S2CPacket>) packets::get);
                if (packet instanceof S2C759LoginSuccessPacket || packet instanceof S2CLoginSuccessPacket || packet instanceof S2CCompressionPacket) {
                    serverType = ServerType.OFFLINE;
                } else if (packet instanceof S2CLoginDisconnectPacket dp) {
                    if (dp.getReason().contains("IP forwarding")) {
                        serverType = ServerType.HACKABLE;
                    } else if (dp.getReason().contains("Forge")) {
                        serverType = ServerType.MODDED;
                    } else if (dp.getReason().contains("whitelisted")) {
                        serverType = ServerType.WHITELISTED;
                    } else if (dp.getReason().contains("Unable to authenticate")) {
                        serverType = ServerType.VELOCITY;
                    } else {
                        System.err.println("Could not determine server type: " + dp.getReason() + " while scanning " + host + ":" + port);
                        serverType = ServerType.UNKNOWN;
                    }
                } else if (packet instanceof S2CEncryptionRequestPacket) {
                    serverType = ServerType.ONLINE;
                } else {
                    throw new RuntimeException("Unknown packet: " + packet);
                }
            }
        } else {
            serverType = ServerType.UNCHECKED;
        }

        return new ScanResult(version, playerCount, maxPlayerCount, onlinePlayers, motd, serverType);
    }

    @SneakyThrows
    @Override
    public void scan(ScanTargetResolver resolver, ScanHandler handler) {
        ScanTarget target;
        while ((target = resolver.next()) != null) {
            String host = target.getHost();
            int port = target.getPort();

            while (executor.getQueue().size() >= queueSize) {
                Thread.sleep(100);
            }

            executor.submit(() -> {
                try {
                    ScanResult result = scan(host, port);
                    if (result == null) return;
                    handler.handle(host, port, result);
                } catch (InvalidStatusException e) {
                    System.err.println("Failed to parse server status: " + e.getMessage() + " while scanning " + host + ":" + port);
                } catch (IOException e) {
                    // ignore
                } catch (Exception e) {
                    System.err.println("Encountered an unexpected exception while scanning " + host + ":" + port);
                    e.printStackTrace();
                }
            });
        }
    }

    @SneakyThrows
    @Override
    public void shutdown() {
        executor.shutdown();
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
    }

    private static class InvalidStatusException extends IOException {

        public InvalidStatusException(String message) {
            super(message);
        }
    }
}
