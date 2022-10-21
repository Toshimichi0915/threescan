package net.toshimichi.threescan;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.Executors;

public class Main {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage: java -jar threescan.jar <host> <portStart> <portEnd> <timeout> <thread>");
            return;
        }

        String host = args[0];
        int portStart = Integer.parseInt(args[1]);
        int portEnd = Integer.parseInt(args[2]);
        ScanRequest request = new ScanRequest(host, portStart, portEnd);

        int timeout = Integer.parseInt(args[3]);
        int thread = Integer.parseInt(args[4]);
        ExecutorScanner scanner = new ExecutorScanner(Executors.newFixedThreadPool(thread), timeout, true);
        scanner.scan(request, Main::showResult);
        scanner.shutdown();
    }

    public static void showResult(ScanRequest request, int port, ScanResult result) {
        JsonObject obj = new JsonObject();
        obj.addProperty("host", request.getHost());
        obj.addProperty("port", port);

        // merge data
        for (Map.Entry<String, JsonElement> entry : gson.toJsonTree(result).getAsJsonObject().entrySet()) {
            obj.add(entry.getKey(), entry.getValue());
        }

        System.out.println(gson.toJson(obj));
    }
}
