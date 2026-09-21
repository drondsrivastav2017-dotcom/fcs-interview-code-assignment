package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@QuarkusTest
public class StoreEndpointTest {

  private static final String PATH = "store";

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @BeforeEach
  public void resetGateway() {
    Mockito.reset(legacyStoreManagerGateway);
  }

  @Test
  public void shouldListTheStoresFromTheInitialData() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("KALLAX"), containsString("TONSTAD"));
  }

  @Test
  public void shouldNotifyTheLegacySystemWithTheCommittedStore() {
    given()
        .contentType(ContentType.JSON)
        .body(storeJson("SYNC-CREATED", 4))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("name", equalTo("SYNC-CREATED"));

    var storeSentToLegacy = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(storeSentToLegacy.capture());

    assertEquals("SYNC-CREATED", storeSentToLegacy.getValue().name);
    assertEquals(4, storeSentToLegacy.getValue().quantityProductsInStock);
    // the identifier is only assigned once the row is persisted
    assertNotNull(storeSentToLegacy.getValue().id);
  }

  @Test
  public void shouldNotNotifyTheLegacySystemWhenTheTransactionIsRolledBack() {
    given()
        .contentType(ContentType.JSON)
        .body(storeJson("SYNC-ROLLED-BACK", 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(any());

    // the store name is unique, so persisting it a second time fails when the transaction commits
    given()
        .contentType(ContentType.JSON)
        .body(storeJson("SYNC-ROLLED-BACK", 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(500);

    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(any());
  }

  @Test
  public void shouldKeepTheStoreWhenTheLegacySystemFails() {
    Mockito.doThrow(new IllegalStateException("legacy system is down"))
        .when(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(any());

    // the data is already committed when the legacy system is called, so the request still succeeds
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body(storeJson("SYNC-LEGACY-DOWN", 2))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().get(PATH + "/" + id).then().statusCode(200).body("name", equalTo("SYNC-LEGACY-DOWN"));
  }

  @Test
  public void shouldUpdateAStoreAndNotifyTheLegacySystem() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body(storeJson("SYNC-UPDATED", 1))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .contentType(ContentType.JSON)
        .body(storeJson("SYNC-UPDATED-RENAMED", 7))
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("SYNC-UPDATED-RENAMED"));

    var storeSentToLegacy = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(storeSentToLegacy.capture());

    // the legacy system receives the persisted entity, identifier included
    assertEquals(id.longValue(), storeSentToLegacy.getValue().id.longValue());
    assertEquals("SYNC-UPDATED-RENAMED", storeSentToLegacy.getValue().name);
    assertEquals(7, storeSentToLegacy.getValue().quantityProductsInStock);
  }

  @Test
  public void shouldPatchAStoreAndNotifyTheLegacySystem() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body(storeJson("SYNC-PATCHED", 3))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    // a quantity of zero is not part of the patch, so the stored value is kept
    given()
        .contentType(ContentType.JSON)
        .body(storeJson("SYNC-PATCHED-RENAMED", 0))
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("SYNC-PATCHED-RENAMED"))
        .body("quantityProductsInStock", equalTo(3));

    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(any());
  }

  @Test
  public void shouldRejectAStoreWithAnIdentifierAlreadySet() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":1,\"name\":\"SYNC-INVALID\",\"quantityProductsInStock\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);

    verifyNoInteractions(legacyStoreManagerGateway);
  }

  @Test
  public void shouldRejectAnUpdateWithoutAName() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":1}")
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":1}")
        .when()
        .patch(PATH + "/1")
        .then()
        .statusCode(422);

    verifyNoInteractions(legacyStoreManagerGateway);
  }

  @Test
  public void shouldReturnNotFoundForAnUnknownStore() {
    given().when().get(PATH + "/999999").then().statusCode(404);

    given()
        .contentType(ContentType.JSON)
        .body(storeJson("SYNC-UNKNOWN", 1))
        .when()
        .put(PATH + "/999999")
        .then()
        .statusCode(404);

    given()
        .contentType(ContentType.JSON)
        .body(storeJson("SYNC-UNKNOWN", 1))
        .when()
        .patch(PATH + "/999999")
        .then()
        .statusCode(404);

    given().when().delete(PATH + "/999999").then().statusCode(404);

    verifyNoInteractions(legacyStoreManagerGateway);
  }

  @Test
  public void shouldDeleteAStore() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body(storeJson("SYNC-DELETED", 1))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  private static String storeJson(String name, int quantityProductsInStock) {
    return """
        {"name":"%s","quantityProductsInStock":%d}""".formatted(name, quantityProductsInStock);
  }
}
