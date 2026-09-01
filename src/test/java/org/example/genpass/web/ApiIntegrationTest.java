package org.example.genpass.web;

import io.restassured.RestAssured;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ApiIntegrationTest {

    private static WebServer server;
    private static int port;

    @BeforeSuite
    public static void startServer() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        server = new WebServer(port, "127.0.0.1");
        server.start();
        RestAssured.baseURI = "http://127.0.0.1:" + port;
        RestAssured.config = RestAssured.config().jsonConfig(
                io.restassured.config.JsonConfig.jsonConfig()
                        .numberReturnType(io.restassured.path.json.config.JsonPathConfig.NumberReturnType.DOUBLE));
    }

    @AfterSuite
    public static void stopServer() {
        server.stop();
    }

    @Test
    public void healthReturnsOk() {
        given().when().get("/api/health")
                .then().statusCode(200)
                .body("status", equalTo("ok"));
    }

    @Test
    public void passwordEndpointReturnsSchema() {
        given().contentType("application/json")
                .body("{\"length\":20,\"lowercase\":true,\"uppercase\":true,\"digits\":true,\"special\":true,\"excludeAmbiguous\":true}")
                .when().post("/api/password")
                .then().statusCode(200)
                .body("result.length()", equalTo(20))
                .body("entropyBits", greaterThan(0d))
                .body("strength", not(emptyString()))
                .body("crackTime", not(emptyString()));
    }

    @Test
    public void passphraseEndpointAppliesOptions() {
        String result = given().contentType("application/json")
                .body("{\"wordCount\":5,\"separator\":\"-\",\"capitalize\":true,\"addDigit\":true}")
                .when().post("/api/passphrase")
                .then().statusCode(200)
                .body("entropyBits", org.hamcrest.Matchers.closeTo(55.02, 0.1))
                .extract().jsonPath().getString("result");

        String[] words = result.split("-");
        assertEquals(words.length, 5);
        int digitWords = 0;
        for (String word : words) {
            assertTrue(Character.isUpperCase(word.charAt(0)), "word not capitalized: " + word);
            if (Character.isDigit(word.charAt(word.length() - 1))) {
                digitWords++;
            }
        }
        assertEquals(digitWords, 1);
    }

    @Test
    public void pinEndpointReturnsDigitsWithoutLeadingZero() {
        given().contentType("application/json")
                .body("{\"length\":6,\"noLeadingZero\":true}")
                .when().post("/api/pin")
                .then().statusCode(200)
                .body("result", matchesPattern("\\d{6}"))
                .body("result", not(startsWith("0")));
    }

    @Test
    public void apiResponsesCarrySecurityHeaders() {
        given().contentType("application/json")
                .body("{\"length\":10,\"lowercase\":true}")
                .when().post("/api/password")
                .then().statusCode(200)
                .header("Cache-Control", equalTo("no-store"))
                .header("Content-Security-Policy", containsString("default-src 'none'"))
                .header("X-Content-Type-Options", equalTo("nosniff"))
                .header("Referrer-Policy", equalTo("no-referrer"))
                .header("X-Frame-Options", equalTo("DENY"));
    }

    @Test
    public void invalidParametersReturn400() {
        given().contentType("application/json")
                .body("{\"length\":3,\"lowercase\":true}")
                .when().post("/api/password")
                .then().statusCode(400)
                .body("error", not(emptyString()));

        given().contentType("application/json")
                .body("{\"length\":10}")
                .when().post("/api/password")
                .then().statusCode(400);

        given().contentType("application/json")
                .body("{\"wordCount\":100}")
                .when().post("/api/passphrase")
                .then().statusCode(400);

        given().contentType("application/json")
                .body("{\"length\":2}")
                .when().post("/api/pin")
                .then().statusCode(400);
    }

    @Test
    public void malformedJsonReturns400() {
        given().contentType("application/json")
                .body("{{{")
                .when().post("/api/password")
                .then().statusCode(400);
    }

    @Test
    public void unknownJsonPropertyReturns400() {
        given().contentType("application/json")
                .body("{\"length\":10,\"lowercase\":true,\"bogus\":1}")
                .when().post("/api/password")
                .then().statusCode(400);
    }

    @Test
    public void floatLengthRejected() {
        given().contentType("application/json")
                .body("{\"length\":4.9,\"lowercase\":true}")
                .when().post("/api/password")
                .then().statusCode(400);
    }

    @Test
    public void stringLengthRejected() {
        given().contentType("application/json")
                .body("{\"length\":\"20\",\"lowercase\":true}")
                .when().post("/api/password")
                .then().statusCode(400);
    }

    @Test
    public void unknownApiPath404CarriesNoStore() {
        given().when().get("/api/unknown")
                .then().statusCode(404)
                .header("Cache-Control", equalTo("no-store"));
    }

    @Test
    public void oversizedBodyReturns413() {
        String big = "{\"length\":10,\"lowercase\":true,\"pad\":\"" + "x".repeat(70_000) + "\"}";
        given().contentType("application/json")
                .body(big)
                .when().post("/api/password")
                .then().statusCode(413);
    }

    @Test
    public void wrongMethodReturns405WithAllow() {
        given().when().get("/api/password")
                .then().statusCode(405)
                .header("Allow", equalTo("POST"));

        given().when().post("/api/health")
                .then().statusCode(405)
                .header("Allow", equalTo("GET"));
    }

    @Test
    public void unknownApiPathReturns404() {
        given().when().get("/api/unknown")
                .then().statusCode(404);

        given().when().get("/api/password/x")
                .then().statusCode(404);
    }

    @Test
    public void staticIndexServesHtml() {
        given().when().get("/")
                .then().statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("Генератор паролей"))
                .header("Cache-Control", equalTo("no-cache"));
    }

    @Test
    public void staticAssetsServedWithTypes() {
        given().when().get("/app.js")
                .then().statusCode(200)
                .contentType(containsString("javascript"));

        given().when().get("/style.css")
                .then().statusCode(200)
                .contentType(containsString("text/css"));

        given().when().get("/missing")
                .then().statusCode(404);
    }

    @Test
    public void headOnIndexReturnsOk() {
        given().when().head("/")
                .then().statusCode(200);
    }
}
