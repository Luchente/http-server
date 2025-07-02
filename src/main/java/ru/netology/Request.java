package ru.netology;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {

    private final String method;
    private final String path;
    private final String query;
    private final Map<String, String> headers;
    private final InputStream body;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> postParams;
    private final Map<String, List<Part>> parts;

    private Request(String method,
                    String path,
                    String query,
                    Map<String, String> headers,
                    InputStream body,
                    Map<String, List<String>> queryParams,
                    Map<String, List<String>> postParams,
                    Map<String, List<Part>> parts) {
        this.method = method;
        this.path = path;
        this.query = query;
        this.headers = Collections.unmodifiableMap(headers);
        this.body = body;
        this.queryParams = queryParams;
        this.postParams = postParams;
        this.parts = parts;
    }

    public static Request parse(BufferedReader reader, InputStream rawBody) throws Exception {
        final var requestLine = reader.readLine();
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

        final var headers = new HashMap<String, String>();
        for (String line = reader.readLine(); !line.equals(""); line = reader.readLine()) {
            final var headerParts = line.split(": ", 2);
            headers.put(headerParts[0], headerParts[1]);
        }

        Map<String, List<String>> postParams = new HashMap<>();
        Map<String, List<Part>> partsMap = new HashMap<>();

        if ("POST".equalsIgnoreCase(method)) {
            String contentType = headers.getOrDefault("Content-Type", "");
            if (contentType.startsWith("application/x-www-form-urlencoded")) {
                var contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
                var bodyBytes = rawBody.readNBytes(contentLength);
                var bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
                List<NameValuePair> bodyPairs = URLEncodedUtils.parse(bodyString, StandardCharsets.UTF_8);
                for (NameValuePair pair : bodyPairs) {
                    postParams.computeIfAbsent(pair.getName(), k -> new ArrayList<>()).add(pair.getValue());
                }
                return new Request(method, path, query, headers, InputStream.nullInputStream(), queryParams, postParams, partsMap);
            } else if (contentType.startsWith("multipart/form-data")) {
                var factory = new DiskFileItemFactory();
                var upload = new ServletFileUpload(factory);
                upload.setHeaderEncoding("UTF-8");
                var context = new RequestContext() {
                    public String getCharacterEncoding() {
                        return "UTF-8";
                    }

                    public String getContentType() {
                        return contentType;
                    }

                    public int getContentLength() {
                        return Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
                    }

                    public InputStream getInputStream() {
                        return rawBody;
                    }
                };

                List<FileItem> items = upload.parseRequest(context);
                for (FileItem item : items) {
                    byte[] content = item.get();
                    Part part = new Part(
                            item.getFieldName(),
                            item.getName(),
                            content,
                            !item.isFormField(),
                            item.getContentType()
                    );
                    partsMap.computeIfAbsent(part.getName(), k -> new ArrayList<>()).add(part);
                }
                return new Request(method, path, query, headers, InputStream.nullInputStream(), queryParams, postParams, partsMap);
            }
        }

        return new Request(method, path, query, headers, rawBody, queryParams, postParams, partsMap);
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

    public String getPostParam(String name) {
        List<String> values = postParams.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    public List<String> getPostParams(String name) {
        return postParams.getOrDefault(name, List.of());
    }

    public Map<String, List<String>> getPostParams() {
        return postParams;
    }

    public Part getPart(String name) {
        List<Part> list = parts.get(name);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public List<Part> getParts(String name) {
        return parts.getOrDefault(name, List.of());
    }

    public Map<String, List<Part>> getParts() {
        return parts;
    }
}

