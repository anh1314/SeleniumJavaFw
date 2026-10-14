package actions.helpers;

import io.qameta.allure.Allure;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static actions.common.BasePage.formatMessage;
import static actions.common.BasePage.logError;
import static actions.common.BasePage.logInfo;
import static io.restassured.RestAssured.given;

public class ApiHelper {

    // ==================== REST CLIENT / REQUEST BUILDER ====================

    public static RequestSpecification getDefaultRequestSpec(String baseUrl, String token, Object... body) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + token);

        if (body != null && body.length > 0 && body[0] != null) {
            builder.setBody(body[0]);
        }
        return builder.build();
    }

    public static Response sendRequest(Method method, String baseUrl, String endpoint, String token, Object body) {
        return given()
                .spec(getDefaultRequestSpec(baseUrl, token, body))
                .when()
                .request(method, endpoint)
                .then()
                .extract()
                .response();
    }

    public static Response sendRequest(Method method, String url, String token, Object body) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(body)
                .when()
                .request(method, url)
                .then()
                .extract()
                .response();
    }

    public static Response sendRequest(Method method, String url, String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .request(method, url)
                .then()
                .extract()
                .response();
    }

    public static Response sendRequest(Method method, String url, Object body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .request(method, url)
                .then()
                .extract()
                .response();
    }

    // ==================== STATUS & LOGGING HELPERS ====================

    public static boolean isSuccessStatusCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    public static String convertToJson(Object value) {
        if (value instanceof Response response) {
            return response.getBody().asPrettyString();
        }
        return JsonHelper.toJson(value);
    }

    public static boolean assertSuccessStatusCode(Response response, String error, Object... args) {
        int statusCode = response.getStatusCode();
        if (!isSuccessStatusCode(statusCode)) {
            String errorMessage = formatMessage(error, args);
            errorMessage += " - status: " + statusCode;
            logError(errorMessage);
            response.prettyPrint();
            return false;
        }
        return true;
    }

    public static boolean assertSuccessStatusCodeRetry(Response response, String error, Object... args) {
        int statusCode = response.getStatusCode();
        if (!isSuccessStatusCode(statusCode)) {
            String errorMessage = formatMessage(error, args);
            errorMessage += " - status: " + statusCode;
            logInfo(errorMessage);
            response.prettyPrint();
            return false;
        }
        return true;
    }

    private static String getStatusIcon(int statusCode) {
        return switch (statusCode / 100) {
            case 2 -> "🟢"; // Success (200, 201, 204...)
            case 3 -> "🔵"; // Redirect (301, 302, 304...)
            case 4 -> "🟠"; // Client Error (400, 401, 403, 404...)
            case 5 -> "🔴"; // Server Error (500, 502, 503...)
            default -> "⚪"; // Other/Unknown Status
        };
    }

    public static void logResponse(Response response, String apiDescription, Object... args) {
        int statusCode = response.getStatusCode();
        String description = formatMessage(apiDescription, args);
        Allure.step(String.format("%s %s", getStatusIcon(statusCode), description), () -> {
            Allure.addAttachment("Response (http %d)".formatted(statusCode), "application/json", response.asPrettyString(), ".json");
        });
    }

    public static void logRequestAndResponse(Object requestBody, Response response, String apiDescription, Object... args) {
        int statusCode = response.getStatusCode();
        String description = formatMessage(apiDescription, args);

        Allure.step(String.format("%s %s", getStatusIcon(statusCode), description), () -> {
            Allure.addAttachment("Request", "application/json", convertToJson(requestBody), ".json");
            Allure.addAttachment("Response (http %d)".formatted(statusCode), "application/json", response.asPrettyString(), ".json");
        });
    }
}
