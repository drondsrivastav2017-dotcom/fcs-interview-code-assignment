package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void shouldCreateUpdateAndRetrieveAProduct() {
    final String path = "product";

    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"PRODUCT-CRUD\",\"description\":\"a chair\",\"stock\":2}")
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .when()
        .get(path + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PRODUCT-CRUD"))
        .body("stock", equalTo(2));

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"PRODUCT-CRUD-RENAMED\",\"stock\":9}")
        .when()
        .put(path + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PRODUCT-CRUD-RENAMED"))
        .body("stock", equalTo(9));

    given().when().delete(path + "/" + id).then().statusCode(204);
    given().when().get(path + "/" + id).then().statusCode(404);
  }

  @Test
  public void shouldRejectInvalidProductRequests() {
    final String path = "product";

    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":1,\"name\":\"PRODUCT-INVALID\"}")
        .when()
        .post(path)
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body("{\"stock\":1}")
        .when()
        .put(path + "/2")
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"PRODUCT-UNKNOWN\"}")
        .when()
        .put(path + "/999999")
        .then()
        .statusCode(404);

    given().when().delete(path + "/999999").then().statusCode(404);
  }
}
