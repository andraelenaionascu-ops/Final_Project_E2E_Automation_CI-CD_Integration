package com.hapifyme.api.utils;

import io.restassured.response.Response;
import java.util.concurrent.TimeUnit;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

public class ApiPoller {

    public static String pollForConfirmationToken(String baseUrl, String email, String apiKey) {
        final String[] token = new String[1];

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Response response = given()
                            .relaxedHTTPSValidation() // Previne blocarea asincrona din cauza certificatului SSL
                            .baseUri(baseUrl)
                            .header("Authorization", apiKey)
                            .queryParam("username_or_email", email)
                            .when()
                            .get("/user/retrieve_token.php")
                            .then()
                            .statusCode(200)
                            .body("confirmation_token", notNullValue())
                            .extract()
                            .response();

                    token[0] = response.jsonPath().getString("confirmation_token");
                });

        return token[0];
    }
}