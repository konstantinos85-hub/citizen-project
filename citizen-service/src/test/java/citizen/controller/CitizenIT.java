package citizen.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import citizen.model.Citizen;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CitizenIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/citizens";
    }

    @Test
    public void testCreateAndGetCitizen() {
        // 1. Δημιουργία αντικειμένου για το POST
        Citizen citizen = new Citizen();
        citizen.setAt("AZ123456");
        citizen.setFirstName("Konstantinos");
        citizen.setLastName("Kouyouris");
        citizen.setGender("Male");
        citizen.setAfm("123456789");
        citizen.setBirthDate("01-01-1990");
        citizen.setAddress("Athens 123");

        // 2. POST request: Δημιουργία Πολίτη
        given()
            .contentType(ContentType.JSON)
            .body(citizen)
        .when()
            .post()
        .then()
            .statusCode(200)
            .body("at", equalTo("AZ123456"))
            .body("firstName", equalTo("Konstantinos"));

        // 3. GET request: Επαλήθευση ότι ο πολίτης υπάρχει
        given()
            .pathParam("at", "AZ123456")
        .when()
            .get("/{at}")
        .then()
            .statusCode(200)
            .body("lastName", equalTo("Kouyouris"));
    }
}
