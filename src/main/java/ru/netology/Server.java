package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class Server {

    private final Set<String> validPaths;
    private final ExecutorService pool = Executors.newFixedThreadPool(64);

    private final ConcurrentMap<String, ConcurrentMap<String, Handler>> handlers = new ConcurrentHashMap<>();

    public Server(List<String> validPaths) {
        this.validPaths = Set.copyOf(validPaths);
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>()).put(path, handler);
    }

    public void listen(int port) throws Exception {
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                final var socket = serverSocket.accept();
                pool.execute(() -> handle(socket));
            }
        }
    }

    private void handle(Socket socket) {
        try (
                socket;
                final var in = socket.getInputStream();
                final var out = new BufferedOutputStream(socket.getOutputStream());
                final var reader = new BufferedReader(new InputStreamReader(in))
        ) {
            final var request = Request.parse(reader, in);

            final var method = request.getMethod();
            final var path = request.getPath(); // чистый путь без query

            final var methodHandlers = handlers.get(method);
            if (methodHandlers != null) {
                final var handler = methodHandlers.get(path);
                if (handler != null) {
                    handler.handle(request, out);
                    return;
                }
            }

            if (!validPaths.contains(path)) { // заменили request.getPath() на path
                final var response = new Response(404, "text/plain", "Not Found");
                response.write(out);
                return;
            }

            final var filePath = Path.of("public", path);
            final var mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);
            out.write(("HTTP/1.1 200 OK\r\n").getBytes());
            out.write(("Content-Type: " + mimeType + "\r\n").getBytes());
            out.write(("Content-Length: " + length + "\r\n").getBytes());
            out.write(("Connection: close\r\n").getBytes());
            out.write(("\r\n").getBytes());
            Files.copy(filePath, out);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
