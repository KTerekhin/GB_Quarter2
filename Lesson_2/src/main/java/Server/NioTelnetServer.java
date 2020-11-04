package Server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

public class NioTelnetServer {

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final String rootPath = "server";

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("localhost", 8189));
        server.configureBlocking(false);
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started!");
        while (server.isOpen()) {
            selector.select();
            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                }
                if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    // TODO: 30.10.2020
    //  ls - список файлов (сделано на уроке),   (DONE)
    //  cd (name) - перейти в папку
    //  touch (name) создать текстовый файл с именем   (DONE)
    //  mkdir (name) создать директорию    (DONE)
    //  rm (name) удалить файл по имени
    //  copy (src, target) скопировать файл из одного пути в другой   (DONE)
    //  cat (name) - вывести в консоль содержимое файла   (DONE)

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
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
        String[] msg = new String(buf, StandardCharsets.UTF_8)
                .replace("\n", "")
                .replace("\r", "")
                .split(" ");

        for (String o : msg) {
            Arrays.toString(msg);
            System.out.print(o + " ");
        }
        System.out.println();

        if (msg[0].equals("end")) {
            System.out.println("client disconnected");
            channel.close();
            key.cancel();
            return;
        }
        if (msg[0].equals("--help")) {
            channel.write(ByteBuffer.wrap("input ls for show file list".getBytes()));
        }
//        if (msg[0].equals("cd")) {
//            changeDir(msg[1]);
//            channel.write(ByteBuffer.wrap(("Вы перешли в папку " + msg[1]).getBytes()));
//        }
        if (msg[0].equals("ls")) {
            channel.write(ByteBuffer.wrap(getFilesList().getBytes()));
        }
        if (msg[0].equals("touch")) {
            createFile(msg[1]);
            channel.write(ByteBuffer.wrap(("Файл " + msg[1] + " создан").getBytes()));
        }
        if (msg[0].equals("mkdir")) {
            createDir(msg[1]);
            channel.write(ByteBuffer.wrap(("Папка " + msg[1] + " создана").getBytes()));
        }
//        if (msg[0].equals("rm")) {
//            deleteFile(msg[1]);
//            channel.write(ByteBuffer.wrap(("Файл " + msg[1] + " удален").getBytes()));
//        }
        if (msg[0].equals("copy")) {
            copyFile(msg[1], msg[2]);
            channel.write(ByteBuffer.wrap(("Файл скопирован в папку " + msg[1]).getBytes()));
        }
        if (msg[0].equals("cat")) {
            readFile(msg[1]);
            channel.write(ByteBuffer.wrap(readFile(msg[1]).getBytes()));
        }
        if (msg[0].equals("curdir")) {   //получение текущей папки
            showCurrentDir();
            channel.write(ByteBuffer.wrap(showCurrentDir().getBytes()));
        }
    }

    private void sendMessage(String message, Selector selector) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                ((SocketChannel) key.channel())
                        .write(ByteBuffer.wrap(message.getBytes()));
            }
        }
    }

    private String getFilesList() {
        return String.join(" ", new File(rootPath).list());
    }

    private void createDir(String dir) throws IOException {
        Files.createDirectory(Paths.get(rootPath, dir));
    }

    private void createFile(String filename) throws IOException {
        Path currentRelativePath = Paths.get(rootPath);
        String s = currentRelativePath.toAbsolutePath().toString();
        Files.createFile(Paths.get(s, filename));
    }

//    private void changeDir(String dir) throws IOException {
//        Path currentPath = Paths.get(rootPath);
//        Path newDir = Paths.get(String.valueOf(currentPath)).relativize(Paths.get(String.valueOf(2)));
//    }

    private void copyFile(String filename, String dir) throws IOException {
        Path src = Paths.get(rootPath, filename);
        Path dst = Paths.get(rootPath, dir, filename);
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    private String showCurrentDir() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }

    private String readFile(String filename) throws IOException {
        return String.join(" ", Files.readAllLines(Paths.get(rootPath, filename)));
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, "LOL");
        channel.write(ByteBuffer.wrap("Enter --help".getBytes()));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}
