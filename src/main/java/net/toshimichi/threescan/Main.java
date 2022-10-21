package net.toshimichi.threescan;

import com.google.gson.Gson;

import java.util.concurrent.Executors;

public class Main {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.out.println("Usage: java -jar threescan.jar <host> <portStart> <portEnd> <timeout> <thread>");
            return;
        }

        String host = args[0];
        int portStart = Integer.parseInt(args[1]);
        int portEnd = Integer.parseInt(args[2]);
        ScanRequest request = new ScanRequest(host, portStart, portEnd);

        int timeout = Integer.parseInt(args[3]);
        int thread = Integer.parseInt(args[4]);
        ExecutorScanner scanner = new ExecutorScanner(Executors.newFixedThreadPool(thread), timeout);
        scanner.scan(request, Main::showResult);
        scanner.shutdown();
    }

    public static void showResult(ScanRequest request, int port, ScanResult result) {
        System.out.println(gson.toJson(result));
    }
}
