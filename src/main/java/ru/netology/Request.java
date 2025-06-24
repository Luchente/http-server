package ru.netology;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Request {

    private final String method;
    private final String path;          // без query-string
    private final String query;         // часть после '?', либо ""
    private final Map<String, String> headers;
    private final InputStream body;

    private Request(String method,
                    String path,
                    String query,
                    Map<String, String> headers,
                    InputStream body) {
        this.method  = method;
        this.path    = path;
        this.query   = query;
        this.headers = Collections.unmodifiableMap(headers);
        this.body    = body;
    }

    public static Request parse(BufferedReader reader, InputStream rawBody) throws Exception {
        /* request-line */
        final var requestLine = reader.readLine();                // "GET /foo?bar=1 HTTP/1.1"
        if (requestLine == null || requestLine.isBlank()) {
            throw new IllegalStateException("Empty request");
        }
        final var parts = requestLine.split(" ");
        if (parts.length != 3) throw new IllegalArgumentException("Malformed request-line");

        final var method = parts[0];
        var fullPath     = parts[1];                              // "/foo?bar=1"

        String path;
        String query;
        final var qIdx = fullPath.indexOf('?');
        if (qIdx >= 0) {
            path  = fullPath.substring(0, qIdx);
            query = fullPath.substring(qIdx + 1);
        } else {
            path  = fullPath;
            query = "";
        }

        /* заголовки */
        final var headers = new HashMap<String, String>();
        String line;
        while (!(line = reader.readLine()).isBlank()) {
            final var colon = line.indexOf(':');
            headers.put(
                    line.substring(0, colon).trim(),
                    line.substring(colon + 1).trim()
            );
        }

        return new Request(method, path, query, headers, rawBody);
    }

    /* ---------- геттеры ---------- */
    public String getMethod()               { return method;  }
    public String getPath()                 { return path;    }
    public String getQuery()                { return query;   }
    public Map<String, String> getHeaders() { return headers; }
    public InputStream getBody()            { return body;    }
}
