package ru.netology;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        var server = new Server(List.of(
                "/index.html", "/spring.svg", "/spring.png", "/resources.html",
                "/styles.css", "/app.js", "/links.html", "/forms.html",
                "/classic.html", "/events.html", "/events.js"
        ));

        // Пример GET-хендлера с query
        server.addHandler("GET", "/echo", (request, out) -> {
            String name = request.getQueryParam("name");
            String text = (name != null) ? name : "no name";
            var response = new Response(200, "text/plain", text);
            response.write(out);
        });

        // POST-хендлер с x-www-form-urlencoded
        server.addHandler("POST", "/submit", (request, out) -> {
            String name = request.getPostParam("name");
            List<String> ranks = request.getPostParams("rank");
            String result = "Name: " + name + ", Ranks: " + String.join(", ", ranks);
            var response = new Response(200, "text/plain", result);
            response.write(out);
        });

        // Новый multipart-хендлер
        server.addHandler("POST", "/upload", (request, out) -> {
            Part namePart = request.getPart("name");
            Part filePart = request.getPart("file");

            String name = (namePart != null) ? namePart.getContentAsString() : "(no name)";
            String fileInfo = (filePart != null)
                    ? "File: " + filePart.getFileName() + ", Size: " + filePart.getContent().length
                    : "(no file)";

            String result = "Received from: " + name + "\n" + fileInfo;
            var response = new Response(200, "text/plain", result);
            response.write(out);
        });

        server.listen(9999);
    }
}