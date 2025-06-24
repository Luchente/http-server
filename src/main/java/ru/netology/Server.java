package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Простой HTTP-сервер с:
 *  • ThreadPool (64);
 *  • динамической регистрацией хендлеров (метод + путь);
 *  • fallback-раздачей статики из каталога /public.
 */
public class Server {

    /* ---------------- конфигурация ---------------- */

    private final Set<String> validPaths;                                 // разрешённые статические файлы
    private final ExecutorService pool = Executors.newFixedThreadPool(64);

    /* handlers:  method  ->  (path -> Handler)  */
    private final ConcurrentMap<String, ConcurrentMap<String, Handler>> handlers = new ConcurrentHashMap<>();

    public Server(List<String> validPaths) {
        this.validPaths = Set.copyOf(validPaths);
    }

    /* ----------- публичное API ----------- */

    public void addHandler(String method, String path, Handler handler) {
        handlers
                .computeIfAbsent(method, m -> new ConcurrentHashMap<>())
                .put(path, handler);
    }

    public void listen(int port) throws IOException {
        try (var serverSocket = new ServerSocket(port)) {
            System.out.println("⇢ Server started on http://localhost:" + port);
            while (!pool.isShutdown()) {
                final var socket = serverSocket.accept();
                pool.execute(() -> handle(socket));
            }
        } finally {
            pool.shutdown();                     // корректно гасим пул при выходе
        }
    }

    /* ----------- обработка одного соединения ----------- */

    void handle(Socket socket) {
        try (socket;
             var in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
             var out = new BufferedOutputStream(socket.getOutputStream())) {

            /* 1. парсим запрос */
            final var request = Request.parse(in, socket.getInputStream());

            /* 2. пробуем найти зарегистрированный хендлер */
            final var methodMap = handlers.get(request.getMethod());
            final var handler = (methodMap == null) ? null : methodMap.get(request.getPath());

            if (handler != null) {
                try {
                    handler.handle(request, out);                 // пользовательский код
                } catch (Exception e) {                           // чтобы падение хендлера не убило поток
                    e.printStackTrace();
                    write500(out);
                }
                return;                                           // ответ уже отправлен
            }

            /* 3. fallback: отдаём статику или 404 */
            serveStaticOr404(request.getPath(), out);

        } catch (Exception e) {
            e.printStackTrace();                                  // I/O failure – просто логируем
        }
    }

    /* ----------- статика ----------- */

    private void serveStaticOr404(String path, BufferedOutputStream out) throws IOException {
        if ("/".equals(path)) path = "/index.html";
        if (!validPaths.contains(path)) {
            write404(out);
            return;
        }

        /* public/index.html  ←  path="/index.html" */
        final var filePath = Path.of("public").resolve(path.substring(1));
        if (Files.notExists(filePath)) {
            write404(out);
            return;
        }

        final var mimeType = Files.probeContentType(filePath);

        /* динамический classic.html */
        if ("/classic.html".equals(path)) {
            final var template = Files.readString(filePath);
            final var body = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
            write200(out, mimeType, body.length);
            out.write(body);
            out.flush();
            return;
        }

        /* обычный файл */
        write200(out, mimeType, Files.size(filePath));
        Files.copy(filePath, out);
        out.flush();
    }

    /* ----------- helpers ----------- */

    private void write200(OutputStream out, String mime, long len) throws IOException {
        var headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mime + "\r\n" +
                "Content-Length: " + len + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(headers.getBytes());
    }

    private void write404(OutputStream out) throws IOException {
        var headers = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(headers.getBytes());
    }

    private void write500(OutputStream out) throws IOException {
        var headers = "HTTP/1.1 500 Internal Server Error\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(headers.getBytes());
    }
}