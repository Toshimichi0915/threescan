package net.toshimichi.threescan;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage: java -jar threescan.jar <host> <portStart> <portEnd> <timeout> <thread>");
            return;
        }

        int portStart = Integer.parseInt(args[1]);
        int portEnd = Integer.parseInt(args[2]);

        ScanTargetResolver targetResolver;
        if (args[0].startsWith("masscan:")) {
            String path = args[0].replaceFirst("masscan:", "");
            targetResolver = new MasscanTargetResolver(Path.of(path));
        } else if (args[0].contains("-")) {
            String[] split = args[0].split("-");
            targetResolver = new MultiScanTargetResolver(split[0], split[1], portStart, portEnd);
        } else if (args[0].contains("/")) {
            String[] split = args[0].split("/");
            targetResolver = new MultiScanTargetResolver(split[0], Integer.parseInt(split[1]), portStart, portEnd);
        } else {
            String host = args[0];
            targetResolver = new SingleScanTargetResolver(host, portStart, portEnd);
        }

        int timeout = Integer.parseInt(args[3]);
        int thread = Integer.parseInt(args[4]);
        int capacity = thread * 2;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(thread, thread, Integer.MAX_VALUE, TimeUnit.DAYS, new ArrayBlockingQueue<>(capacity));
        ExecutorScanner scanner = new ExecutorScanner(executor, capacity, timeout, true);
        scanner.scan(targetResolver, Main::showResult);
        scanner.shutdown();
    }

    public static void showResult(String host, int port, ScanResult result) {
        JsonObject obj = new JsonObject();
        obj.addProperty("host", host);
        obj.addProperty("port", port);

        // merge data
        for (Map.Entry<String, JsonElement> entry : gson.toJsonTree(result).getAsJsonObject().entrySet()) {
            obj.add(entry.getKey(), entry.getValue());
        }

        System.out.println(gson.toJson(obj));
    }
}
