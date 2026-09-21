package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * Covers the warehouse API against the real stack. Every test works on its own business unit codes
 * and locations so that the tests stay independent from each other.
 */
@QuarkusTest
public class WarehouseEndpointTest {

  private static final String PATH = "warehouse";

  @Test
  public void shouldListTheActiveWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("AMSTERDAM-001"));
  }

  @Test
  public void shouldCreateAndRetrieveAWarehouse() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(warehouseJson("MWH.100", "VETSBY-001", 80, 10))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body("businessUnitCode", equalTo("MWH.100"))
            .extract()
            .path("id");

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("location", equalTo("VETSBY-001"))
        .body("capacity", equalTo(80))
        .body("stock", equalTo(10));
  }

  @Test
  public void shouldRejectAWarehouseWithAnAlreadyUsedBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.012", "AMSTERDAM-001", 10, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("already exists"));
  }

  @Test
  public void shouldRejectAWarehouseOnAnUnknownLocation() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.101", "ATLANTIS-001", 10, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("does not exist"));
  }

  @Test
  public void shouldRejectAWarehouseWhenTheLocationIsFull() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.102", "TILBURG-001", 5, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 1 warehouses"));
  }

  @Test
  public void shouldRejectAWarehouseExceedingTheLocationCapacity() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.103", "HELMOND-001", 50, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("still available at location HELMOND-001"));
  }

  @Test
  public void shouldRejectAWarehouseThatCannotHoldItsOwnStock() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.104", "AMSTERDAM-001", 10, 20))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("does not fit in the informed capacity"));
  }

  @Test
  public void shouldRejectIncompleteWarehousePayloads() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson(" ", "AMSTERDAM-001", 10, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("Business unit code is required"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\":\"MWH.105\",\"capacity\":10,\"stock\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("Location is required"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.106", "AMSTERDAM-001", 0, 0))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("Capacity must be a positive number"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.107", "AMSTERDAM-001", 10, -1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("Stock must be zero or a positive number"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"businessUnitCode\":\"MWH.108\",\"location\":\"AMSTERDAM-001\",\"stock\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("Capacity must be a positive number"));
  }

  @Test
  public void shouldArchiveAWarehouse() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(warehouseJson("MWH.200", "EINDHOVEN-001", 20, 5))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH + "/" + id).then().statusCode(404);
    given().when().get(PATH).then().statusCode(200).body(not(containsString("MWH.200")));
  }

  @Test
  public void shouldReturnNotFoundWhenArchivingAnUnknownWarehouse() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void shouldReturnNotFoundForAnUnknownWarehouse() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void shouldRejectAnIdentifierThatIsNotANumber() {
    given()
        .when()
        .get(PATH + "/not-a-number")
        .then()
        .statusCode(400)
        .body("error", containsString("is not a valid identifier"));
  }

  @Test
  public void shouldReplaceAWarehouseKeepingTheBusinessUnitCode() {
    String previousId =
        given()
            .contentType(ContentType.JSON)
            .body(warehouseJson("MWH.300", "ZWOLLE-002", 30, 5))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String newId =
        given()
            .contentType(ContentType.JSON)
            .body(warehouseJson("MWH.300", "ZWOLLE-002", 40, 5))
            .when()
            .post(PATH + "/MWH.300/replacement")
            .then()
            .statusCode(200)
            .body("businessUnitCode", equalTo("MWH.300"))
            .body("capacity", equalTo(40))
            .extract()
            .path("id");

    // the replaced unit is archived and the new one takes over the business unit code
    given().when().get(PATH + "/" + previousId).then().statusCode(404);
    given().when().get(PATH + "/" + newId).then().statusCode(200).body("capacity", equalTo(40));
  }

  @Test
  public void shouldRejectAReplacementOfAnUnknownBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.999", "ZWOLLE-002", 10, 0))
        .when()
        .post(PATH + "/MWH.999/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  public void shouldRejectAReplacementThatDoesNotMatchThePreviousStock() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.310", "AMSTERDAM-002", 20, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.310", "AMSTERDAM-002", 20, 9))
        .when()
        .post(PATH + "/MWH.310/replacement")
        .then()
        .statusCode(400)
        .body("error", containsString("does not match"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouseJson("MWH.310", "AMSTERDAM-002", 3, 3))
        .when()
        .post(PATH + "/MWH.310/replacement")
        .then()
        .statusCode(400)
        .body("error", containsString("cannot accommodate"));
  }

  private static String warehouseJson(
      String businessUnitCode, String location, int capacity, int stock) {
    return """
        {"businessUnitCode":"%s","location":"%s","capacity":%d,"stock":%d}"""
        .formatted(businessUnitCode, location, capacity, stock);
  }
}
