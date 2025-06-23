package ru.netology;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        final var server = new Server(List.of(
                "/index.html", "/spring.svg", "/spring.png", "/resources.html",
                "/styles.css", "/app.js", "/links.html", "/forms.html",
                "/classic.html", "/events.html", "/events.js"
        ));
        server.listen(9999);
    }
}


