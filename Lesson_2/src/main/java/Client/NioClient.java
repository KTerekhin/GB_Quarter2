package Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
*  Попытка создать клиента через NIO
 */

public class NioClient implements Runnable {
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);

    public NioClient() {
        run();
    }

    @Override
    public void run() {
        try {
            startClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startClient() throws IOException {
        SocketChannel socketChannel = openConnection();
        Selector selector = Selector.open();
//      если убрать OP_WRITE, сервер не будет получать сообщения
        socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        while (!Thread.interrupted()) {
            int readyChannels = selector.selectNow();
            if (readyChannels == 0) {
                continue;
            }

            Set<SelectionKey> keySet = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keySet.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey currentKey = keyIterator.next();
                keyIterator.remove();

                if (!currentKey.isValid()) {
                    continue;
                }
                if (currentKey.isConnectable()) {
                    System.out.println("I'm connected to the server!");
                    handleConnectable(currentKey, selector);
                }
                if (currentKey.isWritable()) {
                    handleWritable(currentKey, selector);
                }
                if (currentKey.isReadable()) {
                    handleReadable(currentKey, selector);
                }
            }
        }
    }

    private void handleWritable(SelectionKey key, Selector selector ) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
//        ByteBuffer buffer = ByteBuffer.allocate(512);
        Scanner scanner = new Scanner(System.in);
        String msg = scanner.nextLine();
        buffer.put(msg.getBytes());
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
    }

    private void handleReadable(SelectionKey key, Selector selector) throws IOException{
        SocketChannel channel = (SocketChannel) key.channel();
//        ByteBuffer buffer = ByteBuffer.allocate(512);
        int read = channel.read(buffer);
        if (read == -1) {
            channel.close();
            return;
        }
        if (read == 0) {
            return;
        }
        buffer.flip();
        byte[] buf = new byte[read];
        int pos = 0;
        while (buffer.hasRemaining()) {
            buf[pos++] = buffer.get();
        }
        buffer.clear();
        String msg = new String(buf, StandardCharsets.UTF_8)
                .replace("\n", "")
                .replace("\r", "");
        System.out.println(msg);
    }

    private void handleConnectable(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
//        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ/* | SelectionKey.OP_WRITE*/);  //OP_WRITE ни на что не влияет
    }

    private static SocketChannel openConnection() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 8189));
        socketChannel.configureBlocking(false);
        while (!socketChannel.finishConnect()) {
            System.out.println("waiting connection....");
        }
        return socketChannel;
    }

    public static void main(String[] args) {
        new NioClient();
    }
}
