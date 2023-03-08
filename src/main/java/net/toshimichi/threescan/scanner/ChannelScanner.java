package net.toshimichi.threescan.scanner;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;

@RequiredArgsConstructor
public class ChannelScanner implements Scanner, Runnable {

    private final int connectionTimeout;
    private final int readTimeout;
    private final PacketHandler packetHandler;

    private final BufferPool readBufferPool = new BufferPool(8192);
    private final BufferPool writeBufferPool = new BufferPool(1024);
    private final BufferPool tempBufferPool = new BufferPool(16);
    private final ArrayDeque<ScanContext> queue = new ArrayDeque<>();

    private Thread thread;
    private boolean stopped;

    @Override
    public void scan(ScanTarget target) {
        ByteBuffer readBuffer = readBufferPool.get();
        ByteBuffer writeBuffer = writeBufferPool.get();
        ByteBuffer tempBuffer = tempBufferPool.get();
        ScanContext context = new ScanContext(this, readBuffer, writeBuffer, tempBuffer, target);

        synchronized (queue) {
            queue.add(context);
        }
    }

    @Override
    public void scan(ScanContext context) {
        synchronized (queue) {
            queue.add(context);
        }
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
        try (Selector selector = Selector.open()) {
            while (!stopped || queue.size() > 0) {
                ScanContext poll;
                boolean empty;
                synchronized (queue) {
                    empty = queue.isEmpty();
                    while ((poll = queue.poll()) != null) {
                        ScanTarget target = poll.getScanTarget();
                        SocketChannel channel = SocketChannel.open();
                        channel.configureBlocking(false);
                        channel.connect(new InetSocketAddress(target.getHost(), target.getPort()));

                        SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT, poll);
                        poll.setSelectionKey(key);
                        poll.setStartMs(System.currentTimeMillis());
                    }
                }

                if (empty) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                selector.selectNow();
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
                            context.setReadMs(System.currentTimeMillis());
                            PacketData buf = context.readPacket();
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

                // timeout
                for (SelectionKey key : selector.keys()) {
                    ScanContext context = (ScanContext) key.attachment();
                    boolean timeout = false;
                    if (context.isConnected()) {
                        if (System.currentTimeMillis() - context.getReadMs() > readTimeout) {
                            timeout = true;
                        }
                    } else {
                        if (System.currentTimeMillis() - context.getStartMs() > connectionTimeout) {
                            timeout = true;
                        }
                    }

                    if (timeout) {
                        cancel(context, false);
                    }
                }
            }
        }
    }
}
