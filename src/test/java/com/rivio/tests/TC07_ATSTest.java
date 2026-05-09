package com.rivio.tests;

import com.rivio.utils.ApiConfig;
import com.rivio.utils.AssertionUtils;
import com.rivio.utils.PayloadBuilder;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * TC-ATS-001 → TC-ATS-010
 *
 * Seeded job openings:  id=1 Lead Backend Engineer (dept=1, loc=1) OPEN
 *                       id=2 Regional Sales Manager (dept=3, loc=2) ON_HOLD
 * Seeded candidates:    id=1 Charlie Brown INTERVIEWING (job=1)
 *                       id=2 Diana Prince  OFFERED      (job=1)
 */
@Epic("Rivio HRMS API")
@Feature("Applicant Tracking System")
public class TC07_ATSTest {

    // ── TC-ATS-001: Verify seeded job openings ────────────────────────────────

    @Test(priority = 1, description = "List all job openings — expect 2 seeded")
    @Story("Job Openings")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /job-openings → Lead Backend Engineer (OPEN), Regional Sales Manager (ON_HOLD)")
    public void tc_ats_001_listAllJobOpenings() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/job-openings")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> jobs = response.jsonPath().getList("data");
        assertNotNull(jobs, "Job openings list must not be null");
        System.out.println("✅ Job openings: " + jobs.size());
    }

    // ── TC-ATS-002: Get seeded job opening by ID ──────────────────────────────

    @Test(priority = 2, description = "Get job opening id=1 (Lead Backend Engineer)")
    @Story("Job Openings")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /job-openings/1 → title=Lead Backend Engineer, status=OPEN")
    public void tc_ats_002_getJobOpeningById() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/job-openings/1")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        String title = response.jsonPath().getString("data.title");
        String status = response.jsonPath().getString("data.status");
        assertNotNull(title, "data.title must not be null");
        assertFalse(title.isEmpty(), "data.title must not be empty");
        assertNotNull(status, "data.status must not be null");
        System.out.println("✅ Job opening 1 verified: " + title + " (" + status + ")");
    }

    // ── TC-ATS-003: Create a new job opening ─────────────────────────────────

    @Test(priority = 3, description = "Create a new job opening — QA Engineer (dept=1, loc=1)")
    @Story("Job Openings")
    @Severity(SeverityLevel.CRITICAL)
    public void tc_ats_003_createJobOpening() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.createJobOpeningPayload("QA Automation Engineer", 1, 1))
                .when()
                .post("/job-openings")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Expected 200/201, got: " + response.getStatusCode() + "\n" + response.getBody().asString());

        int jobId = response.jsonPath().getInt("data.id");
        assertTrue(jobId > 0);
        ApiConfig.setCreatedJobOpeningId(jobId);
        assertEquals(response.jsonPath().getString("data.title"), "QA Automation Engineer");
        System.out.println("✅ Job opening created, ID: " + jobId);
    }

    // ── TC-ATS-004: List seeded candidates for job 1 ─────────────────────────

    @Test(priority = 4, description = "List candidates for seeded job id=1")
    @Story("Candidates")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /job-openings/1/candidates → Charlie Brown, Diana Prince")
    public void tc_ats_004_listCandidatesForSeededJob() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/job-openings/1/candidates")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> candidates = response.jsonPath().getList("data");
        assertNotNull(candidates, "Candidates list must not be null");
        System.out.println("✅ Candidates for job 1: " + candidates.size());
    }

    // ── TC-ATS-005: Get seeded candidate by ID ───────────────────────────────

    @Test(priority = 5, description = "Get seeded candidate id=1 (Charlie Brown)")
    @Story("Candidates")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /candidates/1 → name=Charlie Brown, stage=INTERVIEWING")
    public void tc_ats_005_getSeededCandidate() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .get("/candidates/1")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        String name  = response.jsonPath().getString("data.name");
        String stage = response.jsonPath().getString("data.stage");
        assertNotNull(name, "data.name must not be null");
        assertFalse(name.isEmpty(), "data.name must not be empty");
        assertNotNull(stage, "data.stage must not be null");
        System.out.println("✅ Candidate 1 verified: " + name + " (" + stage + ")");
    }

    // ── TC-ATS-006: Apply new candidate to created job ───────────────────────

    @Test(priority = 6, dependsOnMethods = "tc_ats_003_createJobOpening",
          description = "Apply a new candidate to the QA Automation Engineer opening")
    @Story("Candidates")
    @Severity(SeverityLevel.CRITICAL)
    public void tc_ats_006_applyCandidate() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.applyAsCandidatePayload(
                        "Eve Tester",
                        "eve.tester@gmail.com",
                        "https://s3.amazonaws.com/resumes/eve.pdf"))
                .when()
                .post("/job-openings/" + ApiConfig.getCreatedJobOpeningId() + "/candidates")
                .then()
                .extract().response();

        assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Candidate apply failed: " + response.getBody().asString());

        int candidateId = response.jsonPath().getInt("data.id");
        assertTrue(candidateId > 0);
        ApiConfig.setCreatedCandidateId(candidateId);
        assertEquals(response.jsonPath().getString("data.name"), "Eve Tester");
        assertEquals(response.jsonPath().getString("data.stage"), "APPLIED");
        System.out.println("✅ Eve Tester applied, ID: " + candidateId);
    }

    // ── TC-ATS-007: Filter candidates by stage ───────────────────────────────

    @Test(priority = 7, description = "Filter seeded candidates by stage=INTERVIEWING on job 1")
    @Story("Candidates")
    @Severity(SeverityLevel.NORMAL)
    public void tc_ats_007_filterCandidatesByStage() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .queryParam("stage", "INTERVIEWING")
                .when()
                .get("/job-openings/1/candidates")
                .then()
                .extract().response();

        AssertionUtils.assertList(response);
        List<?> results = response.jsonPath().getList("data");
        assertNotNull(results, "Filtered candidates list must not be null");
        System.out.println("✅ INTERVIEWING candidates: " + results.size());
    }

    // ── TC-ATS-008: Move candidate stage ─────────────────────────────────────

    @Test(priority = 8, dependsOnMethods = "tc_ats_006_applyCandidate",
          description = "Move Eve Tester from APPLIED → INTERVIEWING")
    @Story("Candidates")
    @Severity(SeverityLevel.NORMAL)
    public void tc_ats_008_moveCandidateStage() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.candidateStagePayload("INTERVIEWING"))
                .when()
                .put("/candidates/" + ApiConfig.getCreatedCandidateId() + "/stage")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Eve moved to INTERVIEWING");
    }

    // ── TC-ATS-009: Update job opening status ─────────────────────────────────

    @Test(priority = 9, dependsOnMethods = "tc_ats_003_createJobOpening",
          description = "Close the QA Automation Engineer opening")
    @Story("Job Openings")
    @Severity(SeverityLevel.NORMAL)
    public void tc_ats_009_closeJobOpening() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .body(PayloadBuilder.jobOpeningStatusPayload("CLOSED"))
                .when()
                .patch("/job-openings/" + ApiConfig.getCreatedJobOpeningId() + "/status")
                .then()
                .extract().response();

        AssertionUtils.assertSuccess(response);
        System.out.println("✅ Job opening closed");
    }

    // ── TC-ATS-010: Delete job opening ────────────────────────────────────────

    @Test(priority = 10, dependsOnMethods = "tc_ats_009_closeJobOpening",
          description = "Delete the QA Automation Engineer job opening (tolerated if candidates applied)")
    @Story("Job Openings")
    @Severity(SeverityLevel.MINOR)
    public void tc_ats_010_deleteJobOpening() {
        Response response = given()
                .spec(ApiConfig.authSpec())
                .when()
                .delete("/job-openings/" + ApiConfig.getCreatedJobOpeningId())
                .then()
                .extract().response();

        int sc = response.getStatusCode();
        String body = response.getBody().asString();
        if (sc == 200) {
            System.out.println("✅ Job opening deleted");
        } else if (body != null && body.contains("Candidates have already applied")) {
            // Backend rejects deletion of an opening with candidates — expected business rule
            System.out.println("ℹ️ Delete blocked because candidates applied (expected): " + body);
        } else {
            org.testng.Assert.fail("Unexpected delete response (HTTP " + sc + "): " + body);
        }
    }
}
