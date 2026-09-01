package org.example.genpass.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/** Статика из classpath: / → index.html, /app.js, /style.css. Без листинга каталогов. */
public final class StaticHandler {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "/web/index.html", "text/html; charset=UTF-8",
            "/web/app.js", "application/javascript; charset=UTF-8",
            "/web/style.css", "text/css; charset=UTF-8");

    public void handle(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                writeError(exchange, 405);
                return;
            }
            String file = resourceFor(exchange.getRequestURI().getPath());
            if (file == null) {
                writeError(exchange, 404);
                return;
            }
            byte[] content;
            try (InputStream in = StaticHandler.class.getResourceAsStream(file)) {
                if (in == null) {
                    writeError(exchange, 404);
                    return;
                }
                content = in.readAllBytes();
            }
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPES.get(file));
            SecurityHeaders.apply(exchange, false);
            exchange.sendResponseHeaders(200, "HEAD".equals(method) ? -1 : content.length);
            if (!"HEAD".equals(method)) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(content);
                }
            }
        } catch (IOException e) {
            // клиент оборвал соединение
        } finally {
            exchange.close();
        }
    }

    private static String resourceFor(String path) {
        return switch (path) {
            case "/" -> "/web/index.html";
            case "/app.js" -> "/web/app.js";
            case "/style.css" -> "/web/style.css";
            default -> null;
        };
    }

    private static void writeError(HttpExchange exchange, int status) throws IOException {
        SecurityHeaders.apply(exchange, false);
        exchange.sendResponseHeaders(status, -1);
    }
}
