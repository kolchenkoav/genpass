package org.example.genpass.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** JDK HttpServer + пул потоков; маршруты PLAN 5.3. */
public final class WebServer {

    private static final int POOL_SIZE = 4;

    private final HttpServer server;
    private final ExecutorService executor;

    public WebServer(int port, String host) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        executor = Executors.newFixedThreadPool(POOL_SIZE);
        server.setExecutor(executor);

        ApiHandlers api = new ApiHandlers();
        StaticHandler staticHandler = new StaticHandler();
        server.createContext("/api/health", api::health);
        server.createContext("/api/password", api::password);
        server.createContext("/api/passphrase", api::passphrase);
        server.createContext("/api/pin", api::pin);
        server.createContext("/", staticHandler::handle);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
        executor.shutdown();
    }
}
