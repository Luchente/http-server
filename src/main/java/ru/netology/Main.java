package ru.netology;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        var server = new Server(List.of(
                "/index.html", "/spring.svg", "/spring.png", "/resources.html",
                "/styles.css", "/app.js", "/links.html", "/forms.html",
                "/classic.html", "/events.html", "/events.js"
        ));

        // Пример кастомного хендлера
        server.addHandler("GET", "/messages", (req, out) -> {
            var body = "Hello from handler!";
            var response = new Response(200, "text/plain", body);
            response.write(out);
        });

        // Тестовый echo-хендлер для query параметров
        server.addHandler("GET", "/echo", (request, out) -> {
            String name = request.getQueryParam("name");
            String text = (name != null) ? name : "no name";
            var response = new Response(200, "text/plain", text);
            response.write(out);
        });

        server.listen(9999);
    }
}