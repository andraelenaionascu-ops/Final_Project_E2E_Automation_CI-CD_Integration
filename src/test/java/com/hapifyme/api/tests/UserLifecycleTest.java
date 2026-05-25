package com.hapifyme.api.tests;

import com.hapifyme.api.models.UserRequest;
import com.hapifyme.api.utils.ApiPoller;
import com.hapifyme.api.utils.DataGenerator;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Clasa de testare End-to-End (E2E) pentru ciclul complet de viață al unui utilizator
 * în cadrul aplicației HapifyMe.
 * Fluxul acoperă: Înregistrare -> Confirmare Email -> Login -> Profil -> Update -> Delete -> Verificare Inexistență.
 */
public class UserLifecycleTest {

    // URL-ul de bază al instanței de API HapifyMe
    private final String BASE_URL = "https://apps.qualiadept.eu/hapifyme/api";

    // Date de autentificare reutilizabile pe parcursul testului
    private final String dynamicPassword = "SecurePassword123!";
    private String dynamicEmail;

    // Variabile de stare extrase din răspunsuri pentru corelarea pașilor (Data Driven / State Sharing)
    private String apiKey;
    private String userId;
    private String extractedUsername;
    private String confirmationToken;
    private String bearerToken;

    /**
     * Pasul 0: Inițializarea datelor dinamice înaintea rulării testelor.
     * Generăm un email unic pentru a evita conflictele de unicitate la nivel de bază de date (409 Conflict).
     */
    @BeforeClass
    public void setupData() {
        dynamicEmail = DataGenerator.generateRandomEmail();
    }

    /**
     * Pasul 1: Înregistrarea unui utilizator nou.
     * Trimite un payload JSON cu datele noului cont și extrage identificatorii cheie generatori de server.
     */
    @Test(priority = 1)
    public void testRegisterUser() {
        UserRequest registerPayload = new UserRequest("Andra", "QA", dynamicEmail, dynamicPassword);

        Response response = given()
                .relaxedHTTPSValidation() // Ignoră problemele legate de certificatele SSL auto-semnate
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(registerPayload)
                .when()
                .post("/user/register.php")
                .then()
                .statusCode(201) // Cod de succes pentru resurse create
                .body("api_key", notNullValue())
                .body("user_id", notNullValue())
                .extract()
                .response();

        // Extragerea datelor necesare pentru pașii următori
        apiKey = response.jsonPath().getString("api_key");
        userId = response.jsonPath().getString("user_id");
        extractedUsername = response.jsonPath().getString("username");

        // Mecanism de fallback în cazul în care serverul nu returnează username separat
        if (extractedUsername == null) {
            extractedUsername = dynamicEmail;
        }

        System.out.println("Registered User ID: " + userId);
        System.out.println("Extracted Username for Login: " + extractedUsername);
    }

    /**
     * Pasul 2: Preluarea asincronă a token-ului de confirmare.
     * Deoarece trimiterea unui email este o acțiune asincronă în spate, folosim utilitarul ApiPoller
     * bazat pe Awaitility pentru a interoga baza de date/API-ul repetat până apare token-ul.
     */
    @Test(priority = 2, dependsOnMethods = "testRegisterUser")
    public void testGetConfirmationTokenAsync() {
        confirmationToken = ApiPoller.pollForConfirmationToken(BASE_URL, dynamicEmail, apiKey);
        System.out.println("Token extracted successfully: " + confirmationToken);
    }

    /**
     * Pasul 3: Activarea contului / Confirmarea email-ului.
     * Trimite token-ul extras la pasul anterior ca parametru în URL pentru a valida adresa de email.
     */
    @Test(priority = 3, dependsOnMethods = "testGetConfirmationTokenAsync")
    public void testConfirmEmail() {
        given()
                .relaxedHTTPSValidation()
                .baseUri(BASE_URL)
                .queryParam("token", confirmationToken) // Adăugat la URL: ?token=...
                .when()
                .get("/user/confirm_email.php")
                .then()
                .statusCode(200);
    }

    /**
     * Pasul 4: Autentificarea utilizatorului (Login).
     * Folosește credentialele confirmate anterior pentru a genera un token JWT (Json Web Token) securizat.
     */
    @Test(priority = 4, dependsOnMethods = "testConfirmEmail")
    public void testLoginUser() {
        Map<String, String> loginPayload = new HashMap<>();
        loginPayload.put("username", extractedUsername);
        loginPayload.put("password", dynamicPassword);

        Response response = given()
                .relaxedHTTPSValidation()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .when()
                .post("/user/login.php")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .response();

        bearerToken = response.jsonPath().getString("token");
        System.out.println("Login realized successfully. JWT Token: " + bearerToken);
    }

    /**
     * Pasul 5: Citirea profilului utilizatorului (Read).
     * Explicatie securitate: Scriptul PHP `get_profile.php` validează identitatea aplicației prin cheia
     * API trimisă direct în header-ul "Authorization" și aduce detaliile specifice pe baza lui "user_id".
     */
    @Test(priority = 5, dependsOnMethods = "testLoginUser")
    public void testGetProfileAndValidate() {
        given()
                .relaxedHTTPSValidation()
                .baseUri(BASE_URL)
                .header("Authorization", apiKey) // Identificator de securitate la nivel de aplicație
                .queryParam("user_id", userId)     // Identificator resursă la nivel de utilizator
                .when()
                .get("/user/get_profile.php")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                // Aserțiuni ierarhice (navigare în interiorul obiectului JSON "user")
                .body("user.email", equalTo(dynamicEmail))
                .body("user.first_name", equalTo("Andra"));
    }

    /**
     * Pasul 6: Actualizarea profilului (Update).
     * Modifică prenumele utilizatorului și trimite payload-ul prin metoda HTTP PUT.
     */
    @Test(priority = 6, dependsOnMethods = "testGetProfileAndValidate")
    public void testUpdateProfile() {
        UserRequest updatePayload = new UserRequest("Andra-Modificat", "SeniorQA", null, null);

        given()
                .relaxedHTTPSValidation()
                .baseUri(BASE_URL)
                .header("Authorization", apiKey)
                .queryParam("user_id", userId)
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/user/update_profile.php")
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    /**
     * Pasul 7: Ștergerea profilului (Delete).
     * Explicatie securitate: Spre deosebire de citire/modificare, endpoint-ul de ștergere folosește
     * un mecanism strict de tip Bearer Token (JWT), cerând token-ul lung generat la Login.
     */
    @Test(priority = 7, dependsOnMethods = "testUpdateProfile")
    public void testDeleteProfile() {
        given()
                .relaxedHTTPSValidation()
                .baseUri(BASE_URL)
                .header("Authorization", "Bearer " + bearerToken) // Formatul clasic JWT securizat
                .queryParam("user_id", userId)
                .when()
                .delete("/user/delete_profile.php")
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    /**
     * Pasul 8: Test Negativ de Validare a Ștergerii.
     * Încercăm să accesăm din nou profilul abia șters.
     * O aplicație implementată corect trebuie să refuze accesul și să returneze 401 (Unauthorized) sau 404 (Not Found).
     */
    @Test(priority = 8, dependsOnMethods = "testDeleteProfile")
    public void testNegativeProfileCheck() {
        given()
                .relaxedHTTPSValidation()
                .baseUri(BASE_URL)
                .header("Authorization", "Bearer " + bearerToken)
                .queryParam("user_id", userId)
                .when()
                .get("/user/get_profile.php")
                .then()
                .log().ifValidationFails()
                // Acceptă fie 401 fie 404 pentru flexibilitatea implementării de backend
                .statusCode(anyOf(equalTo(401), equalTo(404)));
    }
}