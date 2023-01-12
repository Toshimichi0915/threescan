package net.toshimichi.threescan;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Main {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage: java -jar threescan.jar <type> <timeout> <thread> <name> <uniqueId>");
            return;
        }

        Map<String, Function<BufferedReader, ScanTargetResolver>> resolvers = Map.of(
                "simple", SimpleScanTargetResolver::new,
                "range", RangeScanTargetResolver::new,
                "masscan", MasscanScanTargetResolver::new
        );

        if (!resolvers.containsKey(args[0])) {
            System.err.println("Unknown type: " + args[0]);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        ScanTargetResolver resolver = resolvers.get(args[0]).apply(reader);
        int timeout = Integer.parseInt(args[1]);
        int thread = Integer.parseInt(args[2]);
        int capacity = thread * 2;

        String name = args[3];
        UUID uniqueId = UUID.fromString(args[4]);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(thread, thread, Integer.MAX_VALUE, TimeUnit.DAYS, new ArrayBlockingQueue<>(capacity));
        ExecutorScanner scanner = new ExecutorScanner(executor, capacity, timeout, true, name, uniqueId);
        scanner.scan(resolver, Main::showResult);
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
