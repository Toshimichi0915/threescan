package net.toshimichi.threescan;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.toshimichi.threescan.packet.C2SHandshakePacket;
import net.toshimichi.threescan.packet.C2SStatusPacket;
import net.toshimichi.threescan.packet.PacketInputStream;
import net.toshimichi.threescan.packet.PacketOutputStream;
import net.toshimichi.threescan.packet.S2CStatusPacket;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ExecutorScanner implements Scanner {

    private static final Gson gson = new Gson();
    private final ExecutorService executorService;
    private final int timeout;

    public String parseComponent(JsonArray array) {
        StringBuilder builder = new StringBuilder();
        for (JsonElement element : array) {
            builder.append(element.getAsJsonObject().get("text").getAsString());
        }
        return builder.toString();
    }

    public ScanResult scan(ScanRequest request, int port) throws IOException {
        try (Socket socket = new Socket(request.getHost(), port)) {

            socket.setSoTimeout(timeout);
            PacketInputStream in = new PacketInputStream(socket.getInputStream());
            PacketOutputStream out = new PacketOutputStream(socket.getOutputStream());

            // handshake
            out.writePacket(new C2SHandshakePacket(760, request.getHost(), port, 1));
            out.writePacket(new C2SStatusPacket());

            S2CStatusPacket statusPacket = new S2CStatusPacket();
            in.readPacket(statusPacket);

            JsonObject obj = gson.fromJson(statusPacket.getMessage(), JsonObject.class);
            String version = obj.getAsJsonObject("version").get("name").getAsString();
            int playerCount = obj.getAsJsonObject("players").get("online").getAsInt();
            int maxPlayerCount = obj.getAsJsonObject("players").get("max").getAsInt();
            ArrayList<String> onlinePlayers = new ArrayList<>();
            JsonArray sample = obj.getAsJsonObject("players").getAsJsonArray("sample");
            if (sample != null) {
                for (JsonElement element : sample) {
                    onlinePlayers.add(element.getAsJsonObject().get("name").getAsString());
                }
            }

            JsonObject description = obj.get("description").getAsJsonObject();
            String motd;
            if (description.has("extra")) {
                motd = parseComponent(description.getAsJsonArray("extra"));
            } else {
                motd = description.get("text").getAsString();
            }
            return new ScanResult(version, playerCount, maxPlayerCount, onlinePlayers, motd);
        }
    }

    @Override
    public void scan(ScanRequest request, ScanHandler handler) {
        for (int port = request.getPortStart(); port <= request.getPortEnd(); port++) {
            int fport = port;
            executorService.submit(() -> {
                try {
                    ScanResult result = scan(request, fport);
                    handler.handle(request, fport, result);
                } catch (IOException e) {
                    // ignore
                }
            });
        }
    }

    @SneakyThrows
    @Override
    public void shutdown() {
        executorService.shutdown();
        executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
    }
}
