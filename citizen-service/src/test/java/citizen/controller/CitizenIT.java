package citizen.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import citizen.model.Citizen;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

// Ορισμός RANDOM_PORT για να επιλυθεί η θύρα του server
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CitizenIT {

    // Η σωστή annotation για το 2025 που λύνει το Injection Error
    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/citizens";
    }

    @Test
    public void testCreateAndGetCitizen() {
        Citizen citizen = new Citizen();
        citizen.setAt("AZ123456");
        citizen.setFirstName("Konstantinos");
        citizen.setLastName("Kouyouris");
        citizen.setAfm("123456789");
        citizen.setGender("Male");
        citizen.setBirthDate("01-01-1990");
        citizen.setAddress("Athens 123");

        // 1. POST Request - Δημιουργία
        given()
            .contentType(ContentType.JSON)
            .body(citizen)
        .when()
            .post()
        .then()
            .statusCode(200);

        // 2. GET Request - Επαλήθευση
        given()
            .pathParam("at", "AZ123456")
        .when()
            .get("/{at}")
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Konstantinos"));
    }
}

