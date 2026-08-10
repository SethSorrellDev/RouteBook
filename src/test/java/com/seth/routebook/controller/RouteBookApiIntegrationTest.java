package com.seth.routebook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests across the core API surface (drivers,
 * routes, stops, locations, knowledge entries), run against a real
 * Spring context and the test H2 database seeded by DataSeeder. GET
 * requests remain public; every POST is authenticated with the test
 * admin credentials configured in src/test/resources/application.properties,
 * matching the app's public-read/authenticated-write security model.
 * Attachment upload is intentionally excluded - see AttachmentServiceTest
 * for that coverage with mocked R2 calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RouteBookApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllDrivers_returnsSeededDriver() throws Exception {
        mockMvc.perform(get("/api/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value("EMP-1001"));
    }

    @Test
    void getDriverById_notFound_returnsStructuredErrorResponse() throws Exception {
        mockMvc.perform(get("/api/drivers/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No driver found with id 99999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createDriver_withoutCredentials_returns401() throws Exception {
        Map<String, Object> newDriver = Map.of(
                "employeeId", "EMP-9002",
                "firstName", "No",
                "lastName", "Auth",
                "email", "no.auth@example.com"
        );

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDriver)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required for this operation"));
    }

    @Test
    void createDriver_withWrongCredentials_returns401() throws Exception {
        Map<String, Object> newDriver = Map.of(
                "employeeId", "EMP-9003",
                "firstName", "Wrong",
                "lastName", "Password",
                "email", "wrong@example.com"
        );

        mockMvc.perform(post("/api/drivers")
                        .with(httpBasic("test-admin", "not-the-real-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDriver)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDriver_thenFetchById_succeeds() throws Exception {
        Map<String, Object> newDriver = Map.of(
                "employeeId", "EMP-9001",
                "firstName", "Test",
                "lastName", "Driver",
                "email", "test.driver@example.com"
        );

        String response = mockMvc.perform(post("/api/drivers")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDriver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP-9001"))
                .andReturn().getResponse().getContentAsString();

        Integer newId = objectMapper.readTree(response).get("id").asInt();

        mockMvc.perform(get("/api/drivers/" + newId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"));
    }

    @Test
    void createRoute_withNonexistentDriverId_returns404() throws Exception {
        Map<String, Object> newRoute = Map.of(
                "name", "Bad Route",
                "description", "Should fail",
                "driverId", 99999
        );

        mockMvc.perform(post("/api/routes")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoute)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No driver found with id 99999"));
    }

    @Test
    void createKnowledgeEntry_withBothRouteAndStopId_returns400() throws Exception {
        Map<String, Object> badEntry = Map.of(
                "title", "Bad entry",
                "body", "Should fail",
                "category", "OTHER",
                "routeId", 1,
                "stopId", 1
        );

        mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEntry)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A knowledge entry must target exactly one of routeId or stopId, not both or neither."));
    }

    @Test
    void createKnowledgeEntry_withNeitherRouteNorStopId_returns400() throws Exception {
        Map<String, Object> badEntry = Map.of(
                "title", "Bad entry",
                "body", "Should fail",
                "category", "OTHER"
        );

        mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEntry)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A knowledge entry must target exactly one of routeId or stopId, not both or neither."));
    }

    @Test
    void createKnowledgeEntry_missingTitle_returns400WithFieldError() throws Exception {
        Map<String, Object> badEntry = Map.of(
                "body", "No title provided",
                "category", "OTHER",
                "stopId", 1
        );

        mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEntry)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title is required"));
    }

    @Test
    void fullChain_createLocationStopAndKnowledgeEntry_succeeds() throws Exception {
        Map<String, Object> newLocation = Map.of(
                "addressLine1", "999 Test Way",
                "city", "Kokomo",
                "state", "IN",
                "zipCode", "46901"
        );
        Map<String, Object> newStop = Map.of(
                "customerName", "Test Customer",
                "sequenceOrder", 99,
                "location", newLocation
        );

        String stopResponse = mockMvc.perform(post("/api/routes/1/stops")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStop)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Test Customer"))
                .andReturn().getResponse().getContentAsString();
        int stopId = objectMapper.readTree(stopResponse).get("id").asInt();

        Map<String, Object> newEntry = Map.of(
                "title", "Integration test note",
                "body", "Created during automated test run",
                "category", "CONTACT",
                "stopId", stopId
        );

        mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEntry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stopId").value(stopId));

        mockMvc.perform(get("/api/knowledge-entries").param("stopId", String.valueOf(stopId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Integration test note"));
    }

    @Test
    void verify_withCorrectCredentials_returnsUsername() throws Exception {
        mockMvc.perform(post("/api/auth/verify")
                        .with(httpBasic("test-admin", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test-admin"));
    }

    @Test
    void verify_withoutCredentials_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/verify"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required for this operation"));
    }

    @Test
    void verify_withWrongCredentials_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/verify")
                        .with(httpBasic("test-admin", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRoute_withoutCredentials_returns401() throws Exception {
        mockMvc.perform(delete("/api/routes/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRoute_changesNameAndDescription() throws Exception {
        Map<String, Object> newRoute = Map.of("name", "Route to Update", "description", "Original");
        String createResponse = mockMvc.perform(post("/api/routes")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoute)))
                .andReturn().getResponse().getContentAsString();
        int routeId = objectMapper.readTree(createResponse).get("id").asInt();

        Map<String, Object> updateRequest = Map.of("name", "Updated Name", "description", "Updated description");
        mockMvc.perform(put("/api/routes/" + routeId)
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void deleteRoute_cascadesToStopsAndKnowledgeEntriesAndTheirAttachments() throws Exception {
        // Build an isolated route -> stop -> two knowledge entries (one
        // route-level, one stop-level) so this test never touches the
        // shared seeded data other tests in this class depend on.
        Map<String, Object> newRoute = Map.of("name", "Route to Delete", "description", "temp");
        String routeResponse = mockMvc.perform(post("/api/routes")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoute)))
                .andReturn().getResponse().getContentAsString();
        int routeId = objectMapper.readTree(routeResponse).get("id").asInt();

        Map<String, Object> newLocation = Map.of(
                "addressLine1", "1 Delete Test Rd", "city", "Kokomo", "state", "IN", "zipCode", "46901");
        Map<String, Object> newStop = Map.of(
                "customerName", "Delete Test Stop", "sequenceOrder", 1, "location", newLocation);
        String stopResponse = mockMvc.perform(post("/api/routes/" + routeId + "/stops")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStop)))
                .andReturn().getResponse().getContentAsString();
        int stopId = objectMapper.readTree(stopResponse).get("id").asInt();

        Map<String, Object> routeNote = Map.of(
                "title", "Route note", "body", "x", "category", "OTHER", "routeId", routeId);
        String routeNoteResponse = mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(routeNote)))
                .andReturn().getResponse().getContentAsString();
        int routeNoteId = objectMapper.readTree(routeNoteResponse).get("id").asInt();

        Map<String, Object> stopNote = Map.of(
                "title", "Stop note", "body", "x", "category", "OTHER", "stopId", stopId);
        String stopNoteResponse = mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stopNote)))
                .andReturn().getResponse().getContentAsString();
        int stopNoteId = objectMapper.readTree(stopNoteResponse).get("id").asInt();

        // Delete the route - should cascade through everything above.
        mockMvc.perform(delete("/api/routes/" + routeId)
                        .with(httpBasic("test-admin", "test-password")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/routes/" + routeId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/stops/" + stopId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/knowledge-entries/" + routeNoteId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/knowledge-entries/" + stopNoteId)).andExpect(status().isNotFound());
    }

    @Test
    void updateStop_changesCustomerNameAndSequenceOrder() throws Exception {
        Map<String, Object> newLocation = Map.of(
                "addressLine1", "2 Update Test Rd", "city", "Kokomo", "state", "IN", "zipCode", "46901");
        Map<String, Object> newStop = Map.of(
                "customerName", "Original Name", "sequenceOrder", 5, "location", newLocation);
        String stopResponse = mockMvc.perform(post("/api/routes/1/stops")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStop)))
                .andReturn().getResponse().getContentAsString();
        int stopId = objectMapper.readTree(stopResponse).get("id").asInt();
        int locationId = objectMapper.readTree(stopResponse).get("locationId").asInt();

        Map<String, Object> updateRequest = Map.of(
                "customerName", "Renamed Customer", "sequenceOrder", 9, "locationId", locationId);
        mockMvc.perform(put("/api/stops/" + stopId)
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Renamed Customer"))
                .andExpect(jsonPath("$.sequenceOrder").value(9));
    }

    @Test
    void deleteStop_cascadesToItsKnowledgeEntries() throws Exception {
        Map<String, Object> newLocation = Map.of(
                "addressLine1", "3 Delete Test Rd", "city", "Kokomo", "state", "IN", "zipCode", "46901");
        Map<String, Object> newStop = Map.of(
                "customerName", "Stop To Delete", "sequenceOrder", 1, "location", newLocation);
        String stopResponse = mockMvc.perform(post("/api/routes/1/stops")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStop)))
                .andReturn().getResponse().getContentAsString();
        int stopId = objectMapper.readTree(stopResponse).get("id").asInt();

        Map<String, Object> note = Map.of(
                "title", "Note on stop to delete", "body", "x", "category", "OTHER", "stopId", stopId);
        String noteResponse = mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andReturn().getResponse().getContentAsString();
        int noteId = objectMapper.readTree(noteResponse).get("id").asInt();

        mockMvc.perform(delete("/api/stops/" + stopId)
                        .with(httpBasic("test-admin", "test-password")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stops/" + stopId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/knowledge-entries/" + noteId)).andExpect(status().isNotFound());
    }

    @Test
    void updateKnowledgeEntry_changingTargetFromRouteToStop_succeeds() throws Exception {
        Map<String, Object> note = Map.of(
                "title", "Retargetable note", "body", "x", "category", "OTHER", "routeId", 1);
        String noteResponse = mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andReturn().getResponse().getContentAsString();
        int noteId = objectMapper.readTree(noteResponse).get("id").asInt();

        Map<String, Object> updateRequest = Map.of(
                "title", "Now targets a stop", "body", "y", "category", "HAZARD", "stopId", 1);
        mockMvc.perform(put("/api/knowledge-entries/" + noteId)
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").doesNotExist())
                .andExpect(jsonPath("$.stopId").value(1))
                .andExpect(jsonPath("$.category").value("HAZARD"));
    }

    @Test
    void updateKnowledgeEntry_withBothTargets_returns400() throws Exception {
        Map<String, Object> note = Map.of(
                "title", "Note", "body", "x", "category", "OTHER", "routeId", 1);
        String noteResponse = mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andReturn().getResponse().getContentAsString();
        int noteId = objectMapper.readTree(noteResponse).get("id").asInt();

        Map<String, Object> badUpdate = Map.of(
                "title", "Bad", "body", "x", "category", "OTHER", "routeId", 1, "stopId", 1);
        mockMvc.perform(put("/api/knowledge-entries/" + noteId)
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badUpdate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteKnowledgeEntry_removesIt() throws Exception {
        Map<String, Object> note = Map.of(
                "title", "Note to delete", "body", "x", "category", "OTHER", "routeId", 1);
        String noteResponse = mockMvc.perform(post("/api/knowledge-entries")
                        .with(httpBasic("test-admin", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andReturn().getResponse().getContentAsString();
        int noteId = objectMapper.readTree(noteResponse).get("id").asInt();

        mockMvc.perform(delete("/api/knowledge-entries/" + noteId)
                        .with(httpBasic("test-admin", "test-password")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/knowledge-entries/" + noteId)).andExpect(status().isNotFound());
    }

    @Test
    void search_matchesTitleCaseInsensitively() throws Exception {
        // "gate code" appears only in this entry's title, unlike "nucor"
        // (which also appears in the hazard entry's body text, and would
        // legitimately return two matches - not a bug, just not a good
        // fit for a single-result assertion like this one).
        mockMvc.perform(get("/api/knowledge-entries").param("q", "GATE CODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Nucor Steel gate code"));
    }

    @Test
    void search_matchesBodyTextTooCaseInsensitively() throws Exception {
        // "keypad" only appears in the gate code entry's body, not its title
        mockMvc.perform(get("/api/knowledge-entries").param("q", "KEYPAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Nucor Steel gate code"));
    }

    @Test
    void search_withNoMatches_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/knowledge-entries").param("q", "no-such-term-exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
