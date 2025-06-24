package ru.netology;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        var server = new Server(List.of(
                "/index.html", "/spring.svg", "/spring.png", "/resources.html",
                "/styles.css", "/app.js", "/links.html", "/forms.html",
                "/classic.html", "/events.html", "/events.js"
        ));

        /* пример кастомного хендлера */
        server.addHandler("GET", "/messages", (req, resp) -> {
            var body = "Hello from handler!";
            var response = """
                    HTTP/1.1 200 OK
                    Content-Type: text/plain
                    Content-Length: %d
                    Connection: close
                                        
                    %s""".formatted(body.length(), body);
            resp.write(response.getBytes());
            resp.flush();
        });

        server.listen(9999);
    }
}

