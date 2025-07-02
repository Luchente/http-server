package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {

    private final String method;
    private final String path;          // без query-string
    private final String query;         // часть после '?', либо ""
    private final Map<String, String> headers;
    private final InputStream body;
    private final Map<String, List<String>> queryParams;

    private Request(String method,
                    String path,
                    String query,
                    Map<String, String> headers,
                    InputStream body,
                    Map<String, List<String>> queryParams) {
        this.method = method;
        this.path = path;
        this.query = query;
        this.headers = Collections.unmodifiableMap(headers);
        this.body = body;
        this.queryParams = queryParams;
    }

    public static Request parse(BufferedReader reader, InputStream rawBody) throws Exception {
        /* request-line */
        final var requestLine = reader.readLine();                // "GET /path?name=value HTTP/1.1"
        final var parts = requestLine.split(" ");
        final var method = parts[0];

        final var fullUri = parts[1];
        final var uri = new URI(fullUri);
        final var path = uri.getPath();
        final var query = uri.getQuery() == null ? "" : uri.getQuery();

        Map<String, List<String>> queryParams = new HashMap<>();
        List<NameValuePair> pairs = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
        for (NameValuePair pair : pairs) {
            queryParams.computeIfAbsent(pair.getName(), k -> new ArrayList<>()).add(pair.getValue());
        }

        /* headers */
        final var headers = new HashMap<String, String>();
        for (String line = reader.readLine(); !line.equals(""); line = reader.readLine()) {
            final var headerParts = line.split(": ", 2);
            headers.put(headerParts[0], headerParts[1]);
        }

        return new Request(method, path, query, headers, rawBody, queryParams);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }

    public String getQueryParam(String name) {
        List<String> values = queryParams.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    public List<String> getQueryParams(String name) {
        return queryParams.getOrDefault(name, List.of());
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }
}
