package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final List<String> validPaths;
    private final ExecutorService pool = Executors.newFixedThreadPool(64);

    public Server(List<String> validPath) {
        this.validPaths = List.copyOf(validPath);
    }

    public void listen(int port) {
        try (var serverSocket = new ServerSocket(port)) {
            while (!pool.isShutdown()) {
                final var socket = serverSocket.accept();
                pool.execute(() -> handle(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }

    void handle(Socket socket) {
        try (
                socket;
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            if (parts.length != 3) {
                write404(out);
                return;
            }

            var path = parts[1];
            if ("/".equals(path)) {
                path = "/index.html"; // Default to index.html if root is requested
            }

            if (!validPaths.contains(path)) {
                write404(out);
                return;
            }

            final var filePath = Path.of("public").resolve(path.substring(1));
            final var mimeType = Files.probeContentType(filePath);

            if ("/classic.html".equals(path)) {
                final var template = Files.readString(filePath);
                final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
                write200(out, mimeType, content.length);
                out.write(content);
                return;
            }

            final var length = Files.size(filePath);
            write200(out, mimeType, length);
            Files.copy(filePath, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void write200(OutputStream out, String mimeType, long length) throws IOException {
        out.write(("""
                HTTP/1.1 200 OK\r
                Content-Type: %s\r
                Content-Length: %d\r
                Connection: close\r
                \r
                """).formatted(mimeType, length).getBytes());
    }

    private void write404(OutputStream out) throws IOException {
        out.write(("""
                HTTP/1.1 404 Not Found\r
                Content-Length: 0\r
                Connection: close\r
                \r
                """).getBytes());
    }

}
