package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * Covers the bonus feature: associating warehouses as fulfilment units of products for a store.
 *
 * <p>Each test builds its own stores and products so the constraints are exercised in isolation.
 */
@QuarkusTest
public class FulfillmentEndpointTest {

  private static final String PATH = "fulfillment";

  @Test
  public void shouldAssociateAWarehouseAsFulfilmentUnitOfAProductForAStore() {
    Integer storeId = createStore("FUL-HAPPY-STORE");
    Integer productId = createProduct("FUL-HAPPY-PRODUCT");

    Integer fulfillmentId =
        given()
            .contentType(ContentType.JSON)
            .body(fulfillmentJson(storeId, productId, "MWH.001"))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body("storeName", equalTo("FUL-HAPPY-STORE"))
            .body("productName", equalTo("FUL-HAPPY-PRODUCT"))
            .body("warehouseBusinessUnitCode", equalTo("MWH.001"))
            .extract()
            .path("id");

    given().when().get(PATH + "/store/" + storeId).then().statusCode(200).body("", hasSize(1));
    given()
        .when()
        .get(PATH + "/warehouse/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("FUL-HAPPY-PRODUCT"));
    given().when().get(PATH).then().statusCode(200).body(containsString("FUL-HAPPY-STORE"));

    // the same association cannot be registered twice
    given()
        .contentType(ContentType.JSON)
        .body(fulfillmentJson(storeId, productId, "MWH.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("already fulfilled"));

    given().when().delete(PATH + "/" + fulfillmentId).then().statusCode(204);
    given().when().delete(PATH + "/" + fulfillmentId).then().statusCode(404);
  }

  @Test
  public void shouldAllowAtMostTwoWarehousesPerProductInAStore() {
    Integer storeId = createStore("FUL-RULE1-STORE");
    Integer productId = createProduct("FUL-RULE1-PRODUCT");

    associate(storeId, productId, "MWH.001", 201);
    associate(storeId, productId, "MWH.012", 201);

    given()
        .contentType(ContentType.JSON)
        .body(fulfillmentJson(storeId, productId, "MWH.023"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 2 warehouses"));
  }

  @Test
  public void shouldAllowAtMostThreeWarehousesPerStore() {
    Integer storeId = createStore("FUL-RULE2-STORE");
    Integer firstProductId = createProduct("FUL-RULE2-PRODUCT-A");
    Integer secondProductId = createProduct("FUL-RULE2-PRODUCT-B");

    createWarehouse("FUL.A", "AMSTERDAM-001", 10, 0);

    associate(storeId, firstProductId, "MWH.001", 201);
    associate(storeId, firstProductId, "MWH.012", 201);
    associate(storeId, secondProductId, "MWH.023", 201);

    given()
        .contentType(ContentType.JSON)
        .body(fulfillmentJson(storeId, secondProductId, "FUL.A"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 3 warehouses"));
  }

  @Test
  public void shouldAllowAtMostFiveProductTypesPerWarehouse() {
    Integer storeId = createStore("FUL-RULE3-STORE");
    createWarehouse("FUL.B", "AMSTERDAM-002", 10, 0);

    for (int i = 1; i <= 5; i++) {
      associate(storeId, createProduct("FUL-RULE3-PRODUCT-" + i), "FUL.B", 201);
    }

    given()
        .contentType(ContentType.JSON)
        .body(fulfillmentJson(storeId, createProduct("FUL-RULE3-PRODUCT-6"), "FUL.B"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 5 product types"));
  }

  @Test
  public void shouldRejectAssociationsReferringToUnknownEntities() {
    Integer storeId = createStore("FUL-UNKNOWN-STORE");
    Integer productId = createProduct("FUL-UNKNOWN-PRODUCT");

    associate(999999, productId, "MWH.001", 404);
    associate(storeId, 999999, "MWH.001", 404);
    associate(storeId, productId, "MWH.999", 404);

    given().when().get(PATH + "/store/999999").then().statusCode(404);
    given().when().get(PATH + "/warehouse/MWH.999").then().statusCode(404);
  }

  @Test
  public void shouldRejectAnIncompleteAssociation() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"productId\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("are required"));
  }

  private static void associate(
      Integer storeId, Integer productId, String businessUnitCode, int expectedStatus) {
    given()
        .contentType(ContentType.JSON)
        .body(fulfillmentJson(storeId, productId, businessUnitCode))
        .when()
        .post(PATH)
        .then()
        .statusCode(expectedStatus);
  }

  private static Integer createStore(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body("""
            {"name":"%s","quantityProductsInStock":1}""".formatted(name))
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private static Integer createProduct(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body("""
            {"name":"%s","stock":1}""".formatted(name))
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private static void createWarehouse(
      String businessUnitCode, String location, int capacity, int stock) {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"businessUnitCode":"%s","location":"%s","capacity":%d,"stock":%d}"""
                .formatted(businessUnitCode, location, capacity, stock))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);
  }

  private static String fulfillmentJson(
      Integer storeId, Integer productId, String businessUnitCode) {
    return """
        {"storeId":%d,"productId":%d,"warehouseBusinessUnitCode":"%s"}"""
        .formatted(storeId, productId, businessUnitCode);
  }
}
