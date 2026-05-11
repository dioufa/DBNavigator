package com.db.navigator.integration;

import com.db.navigator.config.TestcontainersConfiguration;
import com.db.navigator.model.dto.StationDTO;
import com.db.navigator.model.entity.Station;
import com.db.navigator.repository.StationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Station Integration Tests")
class StationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StationRepository stationRepository;

    @BeforeEach
    void setUp() {
        stationRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/stations - sollte neuen Bahnhof erstellen")
    void shouldCreateStation() throws Exception {
        StationDTO newStation = StationDTO.builder()
                .code("FRA")
                .name("Frankfurt (Main) Hbf")
                .city("Frankfurt am Main")
                .build();

        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value("FRA"))
                .andExpect(jsonPath("$.name").value("Frankfurt (Main) Hbf"))
                .andExpect(jsonPath("$.city").value("Frankfurt am Main"));

        assertThat(stationRepository.count()).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/v1/stations - sollte alle Bahnhöfe zurückgeben")
    void shouldReturnAllStations() throws Exception {
        // Given
        stationRepository.save(Station.builder()
                .code("FRA").name("Frankfurt Hbf").city("Frankfurt").build());
        stationRepository.save(Station.builder()
                .code("BER").name("Berlin Hbf").city("Berlin").build());

        // When & Then
        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("FRA", "BER")));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/stations/{id} - sollte Bahnhof nach ID finden")
    void shouldFindStationById() throws Exception {
        // Given
        Station saved = stationRepository.save(Station.builder()
                .code("MUC").name("München Hbf").city("München").build());

        // When & Then
        mockMvc.perform(get("/api/v1/stations/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MUC"))
                .andExpect(jsonPath("$.name").value("München Hbf"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/stations/{id} - sollte 404 für nicht existierenden Bahnhof")
    void shouldReturn404ForNonExistingStation() throws Exception {
        mockMvc.perform(get("/api/v1/stations/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v1/stations/code/{code} - sollte Bahnhof nach Code finden")
    void shouldFindStationByCode() throws Exception {
        // Given
        stationRepository.save(Station.builder()
                .code("HAM").name("Hamburg Hbf").city("Hamburg").build());

        // When & Then
        mockMvc.perform(get("/api/v1/stations/code/{code}", "HAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hamburg Hbf"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/stations/search - sollte Bahnhöfe nach Name suchen")
    void shouldSearchStationsByName() throws Exception {
        // Given
        stationRepository.save(Station.builder()
                .code("FRA").name("Frankfurt (Main) Hbf").city("Frankfurt").build());
        stationRepository.save(Station.builder()
                .code("FFO").name("Frankfurt (Oder)").city("Frankfurt").build());
        stationRepository.save(Station.builder()
                .code("BER").name("Berlin Hbf").city("Berlin").build());

        // When & Then
        mockMvc.perform(get("/api/v1/stations/search").param("name", "Frankfurt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", everyItem(containsString("Frankfurt"))));
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/v1/stations/{id} - sollte Bahnhof aktualisieren")
    void shouldUpdateStation() throws Exception {
        // Given
        Station saved = stationRepository.save(Station.builder()
                .code("CGN").name("Köln Hbf").city("Köln").build());

        StationDTO updateDTO = StationDTO.builder()
                .code("CGN")
                .name("Köln Hauptbahnhof")
                .city("Köln")
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/stations/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Köln Hauptbahnhof"));

        Station updated = stationRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Köln Hauptbahnhof");
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/v1/stations/{id} - sollte Bahnhof löschen")
    void shouldDeleteStation() throws Exception {
        // Given
        Station saved = stationRepository.save(Station.builder()
                .code("DUS").name("Düsseldorf Hbf").city("Düsseldorf").build());

        // When & Then
        mockMvc.perform(delete("/api/v1/stations/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(stationRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/v1/stations - sollte 409 bei doppeltem Code")
    void shouldReturn409ForDuplicateCode() throws Exception {
        // Given
        stationRepository.save(Station.builder()
                .code("STR").name("Stuttgart Hbf").city("Stuttgart").build());

        StationDTO duplicate = StationDTO.builder()
                .code("STR")
                .name("Stuttgart Flughafen")
                .city("Stuttgart")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/v1/stations - sollte 400 bei ungültigen Daten")
    void shouldReturn400ForInvalidData() throws Exception {
        StationDTO invalid = StationDTO.builder()
                .code("")  // Ungültig: leer
                .name("Test")
                .city("Test")
                .build();

        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}