package net.toshimichi.threescan.scanner;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
public class ChannelScanner implements Scanner, Runnable {

    private static final int SELECT_TIMEOUT_MS = 10;
    private static final int SWEEP_INTERVAL_MS = 50;

    private final int connectionTimeout;
    private final int readTimeout;
    private final PacketHandler packetHandler;

    private final BufferPool readBufferPool = new BufferPool(16384);
    private final BufferPool writeBufferPool = new BufferPool(1024);
    private final BufferPool tempBufferPool = new BufferPool(16);

    private final Queue<ScanContext> queue = new ConcurrentLinkedQueue<>();
    private final Selector selector = openSelector();

    private Thread thread;
    private long lastSweepMs;
    private volatile boolean stopped;

    @SneakyThrows
    private static Selector openSelector() {
        return Selector.open();
    }

    @Override
    public void scan(ScanTarget target) {
        ByteBuffer readBuffer = readBufferPool.get();
        ByteBuffer writeBuffer = writeBufferPool.get();
        ByteBuffer tempBuffer = tempBufferPool.get();

        scan(new ScanContext(this, readBuffer, writeBuffer, tempBuffer, target));
    }

    @Override
    public void scan(ScanContext context) {
        queue.add(context);
        selector.wakeup();
    }

    @Override
    public void start() {
        thread = new Thread(this);
        thread.setName("channel-scanner-" + thread.getId());
        thread.start();
    }

    @Override
    public void stop() throws InterruptedException {
        stopped = true;
        selector.wakeup();
        thread.join();
    }

    private void cancel(ScanContext context, boolean reused) throws IOException {
        context.disconnect(reused);
        if (!context.isCancelled() && !reused) {
            context.setCancelled(true);
            readBufferPool.release(context.getReadBuffer());
            writeBufferPool.release(context.getWriteBuffer());
            tempBufferPool.release(context.getTempBuffer());
        }
    }

    @SneakyThrows
    @Override
    public void run() {
        try {
            while (!stopped || !queue.isEmpty() || !selector.keys().isEmpty()) {
                ScanContext poll;
                while ((poll = queue.poll()) != null) {
                    ScanTarget target = poll.getScanTarget();
                    SocketChannel channel = SocketChannel.open();
                    channel.configureBlocking(false);
                    channel.setOption(StandardSocketOptions.SO_LINGER, 0);
                    channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                    channel.connect(new InetSocketAddress(target.getHost(), target.getPort()));

                    SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT, poll);
                    poll.setSelectionKey(key);
                    poll.setStartMs(System.currentTimeMillis());
                }

                selector.select(SELECT_TIMEOUT_MS);

                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    ScanContext context = (ScanContext) key.attachment();
                    SocketChannel channel = (SocketChannel) key.channel();
                    try {
                        if (key.isConnectable() && !context.isConnected()) {
                            if (!channel.finishConnect()) {
                                throw new IOException();
                            }
                            context.setConnected(true);
                            context.setReadMs(System.currentTimeMillis());
                            key.interestOps(SelectionKey.OP_READ);

                            packetHandler.onConnected(context);
                        }

                        if (key.isReadable()) {
                            int prevPos = context.getReadBuffer().position();
                            PacketData buf = context.readPacket();

                            int pos = context.getReadBuffer().position();
                            if (prevPos != pos) {
                                context.setReadMs(System.currentTimeMillis());
                            }

                            if (buf != null) {
                                packetHandler.onPacketReceived(context, buf);
                                context.getReadBuffer().clear();
                            }
                        }

                        if (!key.isValid()) {
                            cancel(context, context.isReused());
                        }

                    } catch (InvalidStatusException e) {
                        System.err.println(e.getMessage());
                        cancel(context, false);
                    } catch (IOException e) {
                        // probably an invalid server
                        cancel(context, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        cancel(context, false);
                    }
                }

                long now = System.currentTimeMillis();
                if (now - lastSweepMs < SWEEP_INTERVAL_MS) continue;
                lastSweepMs = now;

                for (SelectionKey key : selector.keys()) {
                    ScanContext context = (ScanContext) key.attachment();

                    if (context.isCancelled() || !key.isValid()) continue;

                    long deadline = context.isConnected()
                            ? context.getReadMs() + readTimeout
                            : context.getStartMs() + connectionTimeout;

                    if (now > deadline) {
                        cancel(context, false);
                    }
                }
            }
        } finally {
            selector.close();
        }
    }
}
