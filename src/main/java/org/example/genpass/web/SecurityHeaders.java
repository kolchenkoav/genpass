package org.example.genpass.web;

import com.sun.net.httpserver.HttpExchange;

/** Заголовки безопасности на всех ответах (PLAN 3.4–3.5): API — no-store, статика — no-cache. */
final class SecurityHeaders {

    static final String CSP = "default-src 'none'; script-src 'self'; style-src 'self'; "
            + "connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'";

    private SecurityHeaders() {
    }

    static void apply(HttpExchange exchange, boolean noStore) {
        exchange.getResponseHeaders().set("Cache-Control", noStore ? "no-store" : "no-cache");
        exchange.getResponseHeaders().set("Content-Security-Policy", CSP);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
    }
}
