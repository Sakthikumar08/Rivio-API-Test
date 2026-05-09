package com.rivio.utils;

import io.restassured.response.Response;

import static org.testng.Assert.*;

/**
 * Shared assertion helpers that enforce the Rivio standard response wrapper:
 * { "success": true, "message": "...", "data": {...}, "timestamp": "..." }
 */
public class AssertionUtils {

    /** Assert the wrapper envelope is present and success == true */
    public static void assertSuccess(Response response) {
        assertSuccess(response, 200);
    }

    public static void assertSuccess(Response response, int expectedStatus) {
        assertEquals(response.getStatusCode(), expectedStatus,
                "Unexpected HTTP status. Body: " + response.getBody().asString());
        assertTrue(response.jsonPath().getBoolean("success"),
                "Expected success=true but got: " + response.getBody().asString());
        assertNotNull(response.jsonPath().getString("message"),
                "Response wrapper missing 'message' field");
    }

    /** Assert success and return the 'data' node for further assertions */
    public static io.restassured.path.json.JsonPath assertSuccessAndGetData(Response response) {
        assertSuccess(response);
        return response.jsonPath();
    }

    /** Assert success and extract data.id as int */
    public static int assertSuccessAndGetId(Response response) {
        assertSuccess(response);
        int id = response.jsonPath().getInt("data.id");
        assertTrue(id > 0, "Expected positive id in data.id, got: " + id);
        return id;
    }

    /** Assert failure response */
    public static void assertFailure(Response response, int expectedStatus) {
        assertEquals(response.getStatusCode(), expectedStatus,
                "Expected failure status " + expectedStatus + " but got: "
                        + response.getStatusCode() + ". Body: " + response.getBody().asString());
    }

    /** Assert paginated response structure (Spring Page or custom wrapper) */
    public static void assertPaginated(Response response) {
        assertSuccess(response);
        assertNotNull(response.jsonPath().get("data.content"), "Missing data.content in paginated response");
        // Spring's Page exposes `number`; custom wrappers sometimes expose `pageNumber`. Accept either.
        Object pageIdx = response.jsonPath().get("data.pageNumber");
        if (pageIdx == null) pageIdx = response.jsonPath().get("data.number");
        if (pageIdx == null) pageIdx = response.jsonPath().get("data.pageable.pageNumber");
        assertNotNull(pageIdx, "Missing page index (data.pageNumber / data.number / data.pageable.pageNumber)");
        assertNotNull(response.jsonPath().get("data.totalElements"), "Missing data.totalElements");
    }

    /** Assert a list response (non-paginated array in data) */
    public static void assertList(Response response) {
        assertSuccess(response);
        assertNotNull(response.jsonPath().get("data"), "data field is null");
    }
}
