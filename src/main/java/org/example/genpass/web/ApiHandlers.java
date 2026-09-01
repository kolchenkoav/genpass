package org.example.genpass.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import org.example.genpass.core.PassphraseGenerator;
import org.example.genpass.core.PassphraseOptions;
import org.example.genpass.core.PasswordGenerator;
import org.example.genpass.core.PasswordOptions;
import org.example.genpass.core.PinGenerator;
import org.example.genpass.core.PinOptions;
import org.example.genpass.core.StrengthEstimator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** JSON API: POST /api/password|passphrase|pin, GET /api/health. Результат генерации не логируется. */
public final class ApiHandlers {

    private static final int MAX_BODY_BYTES = 64 * 1024;

    // Строгая коэрсия: никаких строк→int и float→int (дробная длина молча бы усекалась)
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
    private final PasswordGenerator passwordGenerator = new PasswordGenerator();
    private final PassphraseGenerator passphraseGenerator = new PassphraseGenerator();
    private final PinGenerator pinGenerator = new PinGenerator();

    public void health(HttpExchange exchange) {
        try {
            if (!"/api/health".equals(exchange.getRequestURI().getPath())) {
                notFound(exchange);
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                methodNotAllowed(exchange, "GET");
                return;
            }
            writeJson(exchange, 200, new HealthResponse("ok"));
        } catch (IOException e) {
            // клиент оборвал соединение — ответить уже некому
        } finally {
            exchange.close();
        }
    }

    public void password(HttpExchange exchange) throws IOException {
        handlePost(exchange, "/api/password", body -> {
            PasswordRequest req = mapper.readValue(body, PasswordRequest.class);
            PasswordOptions options = new PasswordOptions(req.length(), req.lowercase(), req.uppercase(),
                    req.digits(), req.special(), req.excludeAmbiguous());
            String result = passwordGenerator.generate(options);
            double bits = StrengthEstimator.entropyBits(options);
            return new ApiResponse(result, bits, StrengthEstimator.strengthLabel(bits),
                    StrengthEstimator.crackTime(bits));
        });
    }

    public void passphrase(HttpExchange exchange) throws IOException {
        handlePost(exchange, "/api/passphrase", body -> {
            PassphraseRequest req = mapper.readValue(body, PassphraseRequest.class);
            PassphraseOptions options = new PassphraseOptions(req.wordCount(), req.separator(),
                    req.capitalize(), req.addDigit());
            String result = passphraseGenerator.generate(options);
            double bits = StrengthEstimator.entropyBits(options);
            return new ApiResponse(result, bits, StrengthEstimator.strengthLabel(bits),
                    StrengthEstimator.crackTime(bits));
        });
    }

    public void pin(HttpExchange exchange) throws IOException {
        handlePost(exchange, "/api/pin", body -> {
            PinRequest req = mapper.readValue(body, PinRequest.class);
            PinOptions options = new PinOptions(req.length(), req.noLeadingZero());
            String result = pinGenerator.generate(options);
            double bits = StrengthEstimator.entropyBits(options);
            return new ApiResponse(result, bits, StrengthEstimator.strengthLabel(bits),
                    StrengthEstimator.crackTime(bits));
        });
    }

    private interface BodyHandler {
        Object apply(byte[] body) throws IOException;
    }

    private void handlePost(HttpExchange exchange, String path, BodyHandler handler) throws IOException {
        try {
            if (!path.equals(exchange.getRequestURI().getPath())) {
                notFound(exchange);
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                methodNotAllowed(exchange, "POST");
                return;
            }
            byte[] body = readBody(exchange);
            writeJson(exchange, 200, handler.apply(body));
        } catch (RequestBodyTooLargeException e) {
            writeJson(exchange, 413, new ErrorResponse("request body too large"));
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 400, new ErrorResponse(e.getMessage()));
        } catch (JsonProcessingException e) {
            writeJson(exchange, 400, new ErrorResponse("invalid JSON"));
        } catch (IOException e) {
            // клиент оборвал соединение
        } catch (RuntimeException e) {
            writeJson(exchange, 500, new ErrorResponse("internal error"));
        } finally {
            exchange.close();
        }
    }

    private byte[] readBody(HttpExchange exchange) throws IOException, RequestBodyTooLargeException {
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > MAX_BODY_BYTES) {
                throw new RequestBodyTooLargeException();
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private void methodNotAllowed(HttpExchange exchange, String allow) throws IOException {
        exchange.getResponseHeaders().set("Allow", allow);
        writeJson(exchange, 405, new ErrorResponse("method not allowed"));
    }

    private void notFound(HttpExchange exchange) throws IOException {
        writeJson(exchange, 404, new ErrorResponse("not found"));
    }

    private void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        SecurityHeaders.apply(exchange, true);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static final class RequestBodyTooLargeException extends IOException {
    }

    private record PasswordRequest(int length, boolean lowercase, boolean uppercase,
                                   boolean digits, boolean special, boolean excludeAmbiguous) {
    }

    private record PassphraseRequest(int wordCount, String separator, boolean capitalize, boolean addDigit) {
    }

    private record PinRequest(int length, boolean noLeadingZero) {
    }

    private record ApiResponse(String result, double entropyBits, String strength, String crackTime) {
    }

    private record HealthResponse(String status) {
    }

    private record ErrorResponse(String error) {
    }
}
