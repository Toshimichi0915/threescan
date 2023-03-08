package net.toshimichi.threescan;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.toshimichi.threescan.resolver.MasscanScanTargetResolver;
import net.toshimichi.threescan.resolver.RangeScanTargetResolver;
import net.toshimichi.threescan.resolver.ScanTargetResolver;
import net.toshimichi.threescan.resolver.SimpleScanTargetResolver;
import net.toshimichi.threescan.resolver.ThreescanScanTargetResolver;
import net.toshimichi.threescan.scanner.ChannelScanner;
import net.toshimichi.threescan.scanner.MultiScanner;
import net.toshimichi.threescan.scanner.RateLimitScanner;
import net.toshimichi.threescan.scanner.ScanMode;
import net.toshimichi.threescan.scanner.ScanPacketHandler;
import net.toshimichi.threescan.scanner.ScanTarget;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class Main {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("Usage: java -jar threescan.jar <type> <mode> <timeout> <rate> <name> <uniqueId>");
            return;
        }

        Map<String, Function<BufferedReader, ScanTargetResolver>> resolvers = Map.of(
                "simple", SimpleScanTargetResolver::new,
                "range", RangeScanTargetResolver::new,
                "masscan", MasscanScanTargetResolver::new,
                "threescan", ThreescanScanTargetResolver::new
        );

        if (!resolvers.containsKey(args[0])) {
            System.err.println("Unknown type: " + args[0]);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        ScanTargetResolver resolver = resolvers.get(args[0]).apply(reader);

        ScanMode scanMode;
        try {
            scanMode = ScanMode.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown mode: " + args[1]);
            return;
        }

        int timeout = Integer.parseInt(args[2]);
        int rate = Integer.parseInt(args[3]);

        String name = args[4];
        UUID uniqueId = UUID.fromString(args[5]);

        MultiScanner multiScanner = new MultiScanner(
                () -> new ChannelScanner(timeout, timeout, new ScanPacketHandler(scanMode, Main::showResult, name, uniqueId))
        );

        RateLimitScanner rateLimitScanner = new RateLimitScanner(rate * 0.001, multiScanner);
        rateLimitScanner.start();

        ScanTarget target;
        while ((target = resolver.next()) != null) {
            rateLimitScanner.scan(target);
        }

        rateLimitScanner.stop();
    }

    public static void showResult(ScanTarget target, ScanResult result) {
        JsonObject targetObj = gson.toJsonTree(target).getAsJsonObject();
        JsonObject resultObj = gson.toJsonTree(result).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : resultObj.entrySet()) {
            targetObj.add(entry.getKey(), entry.getValue());
        }

        System.out.println(gson.toJson(targetObj));
    }
}
