package org.example.genpass;

import org.example.genpass.web.WebServer;

import java.io.IOException;

public final class App {

    private static final String FALLBACK_VERSION = "1.0-SNAPSHOT";

    private App() {
    }

    public static void main(String[] args) {
        try {
            int port = parsePort(System.getenv("PORT"));
            String host = System.getenv().getOrDefault("HOST", "0.0.0.0");
            WebServer server = new WebServer(port, host);
            server.start();
            System.out.println("genpass " + version() + " listening on http://" + host + ":" + port);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("genpass: " + e.getMessage());
            System.exit(1);
        }
    }

    static int parsePort(String value) {
        if (value == null || value.isBlank()) {
            return 8080;
        }
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("PORT must be an integer between 1 and 65535: " + value);
        }
    }

    static String version() {
        String v = App.class.getPackage().getImplementationVersion();
        return v != null ? v : FALLBACK_VERSION;
    }
}
