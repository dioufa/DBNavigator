package com.db.navigator.controller;

import com.db.navigator.exception.DuplicateResourceException;
import com.db.navigator.exception.GlobalExceptionHandler;
import com.db.navigator.exception.ResourceNotFoundException;
import com.db.navigator.model.dto.StationDTO;
import com.db.navigator.service.StationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StationController Tests")
class StationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StationService stationService;

    @InjectMocks
    private StationController stationController;

    private ObjectMapper objectMapper;
    private StationDTO frankfurtDTO;
    private StationDTO berlinDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(stationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        frankfurtDTO = StationDTO.builder()
                .id(1L)
                .code("FRA")
                .name("Frankfurt Hbf")
                .city("Frankfurt")
                .build();

        berlinDTO = StationDTO.builder()
                .id(2L)
                .code("BER")
                .name("Berlin Hbf")
                .city("Berlin")
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/stations Tests")
    class GetAllStationsTests {

        @Test
        @DisplayName("sollte alle Bahnhöfe zurückgeben")
        void shouldReturnAllStations() throws Exception {
            when(stationService.findAll()).thenReturn(List.of(frankfurtDTO, berlinDTO));

            mockMvc.perform(get("/api/v1/stations"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].code", is("FRA")))
                    .andExpect(jsonPath("$[1].code", is("BER")));
        }

        @Test
        @DisplayName("sollte leere Liste zurückgeben")
        void shouldReturnEmptyList() throws Exception {
            when(stationService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/stations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stations/{id} Tests")
    class GetStationByIdTests {

        @Test
        @DisplayName("sollte Bahnhof nach ID zurückgeben")
        void shouldReturnStationById() throws Exception {
            when(stationService.findById(1L)).thenReturn(frankfurtDTO);

            mockMvc.perform(get("/api/v1/stations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("FRA")))
                    .andExpect(jsonPath("$.name", is("Frankfurt Hbf")));
        }

        @Test
        @DisplayName("sollte 404 zurückgeben wenn nicht gefunden")
        void shouldReturn404WhenNotFound() throws Exception {
            when(stationService.findById(99L))
                    .thenThrow(new ResourceNotFoundException("Station nicht gefunden mit id = 99"));

            mockMvc.perform(get("/api/v1/stations/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stations/code/{code} Tests")
    class GetStationByCodeTests {

        @Test
        @DisplayName("sollte Bahnhof nach Code zurückgeben")
        void shouldReturnStationByCode() throws Exception {
            when(stationService.findByCode("FRA")).thenReturn(frankfurtDTO);

            mockMvc.perform(get("/api/v1/stations/code/FRA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("FRA")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stations/search Tests")
    class SearchStationsTests {

        @Test
        @DisplayName("sollte Bahnhöfe nach Name suchen")
        void shouldSearchStationsByName() throws Exception {
            when(stationService.searchByName("Frank")).thenReturn(List.of(frankfurtDTO));

            mockMvc.perform(get("/api/v1/stations/search").param("name", "Frank"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", containsString("Frankfurt")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/stations Tests")
    class CreateStationTests {

        @Test
        @DisplayName("sollte neuen Bahnhof erstellen")
        void shouldCreateStation() throws Exception {
            StationDTO newStation = StationDTO.builder()
                    .code("MUC")
                    .name("München Hbf")
                    .city("München")
                    .build();

            StationDTO createdStation = StationDTO.builder()
                    .id(3L)
                    .code("MUC")
                    .name("München Hbf")
                    .city("München")
                    .build();

            when(stationService.create(any(StationDTO.class))).thenReturn(createdStation);

            mockMvc.perform(post("/api/v1/stations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newStation)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.code", is("MUC")));
        }

        @Test
        @DisplayName("sollte 409 zurückgeben bei doppeltem Code")
        void shouldReturn409WhenDuplicateCode() throws Exception {
            when(stationService.create(any(StationDTO.class)))
                    .thenThrow(new DuplicateResourceException("Station mit Code FRA existiert bereits"));

            mockMvc.perform(post("/api/v1/stations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(frankfurtDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", is("Conflict")));
        }

        @Test
        @DisplayName("sollte 400 zurückgeben bei ungültigen Daten")
        void shouldReturn400WhenInvalidData() throws Exception {
            StationDTO invalidStation = StationDTO.builder()
                    .code("")  // Ungültig: leer
                    .name("Test")
                    .city("Test")
                    .build();

            mockMvc.perform(post("/api/v1/stations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidStation)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/stations/{id} Tests")
    class UpdateStationTests {

        @Test
        @DisplayName("sollte Bahnhof aktualisieren")
        void shouldUpdateStation() throws Exception {
            StationDTO updateDTO = StationDTO.builder()
                    .code("FRA")
                    .name("Frankfurt (Main) Hbf")
                    .city("Frankfurt am Main")
                    .build();

            StationDTO updatedStation = StationDTO.builder()
                    .id(1L)
                    .code("FRA")
                    .name("Frankfurt (Main) Hbf")
                    .city("Frankfurt am Main")
                    .build();

            when(stationService.update(eq(1L), any(StationDTO.class))).thenReturn(updatedStation);

            mockMvc.perform(put("/api/v1/stations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Frankfurt (Main) Hbf")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/stations/{id} Tests")
    class DeleteStationTests {

        @Test
        @DisplayName("sollte Bahnhof löschen")
        void shouldDeleteStation() throws Exception {
            doNothing().when(stationService).delete(1L);

            mockMvc.perform(delete("/api/v1/stations/1"))
                    .andExpect(status().isNoContent());

            verify(stationService).delete(1L);
        }

        @Test
        @DisplayName("sollte 404 zurückgeben wenn nicht gefunden")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Station nicht gefunden"))
                    .when(stationService).delete(99L);

            mockMvc.perform(delete("/api/v1/stations/99"))
                    .andExpect(status().isNotFound());
        }
    }
}