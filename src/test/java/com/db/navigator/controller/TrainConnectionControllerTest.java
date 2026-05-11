package com.db.navigator.controller;

import com.db.navigator.exception.GlobalExceptionHandler;
import com.db.navigator.exception.ResourceNotFoundException;
import com.db.navigator.model.dto.TrainConnectionDTO;
import com.db.navigator.model.entity.TrainType;
import com.db.navigator.service.TrainConnectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainConnectionController Tests")
class TrainConnectionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TrainConnectionService connectionService;

    @InjectMocks
    private TrainConnectionController connectionController;

    private ObjectMapper objectMapper;
    private TrainConnectionDTO iceConnectionDTO;
    private TrainConnectionDTO reConnectionDTO;
    private LocalDateTime departureTime;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(connectionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        departureTime = LocalDateTime.of(2026, 2, 10, 8, 0);

        iceConnectionDTO = TrainConnectionDTO.builder()
                .id(1L)
                .trainNumber("ICE 123")
                .trainType(TrainType.ICE)
                .departureStationId(1L)
                .departureStationName("Frankfurt Hbf")
                .departureStationCode("FRA")
                .arrivalStationId(2L)
                .arrivalStationName("Berlin Hbf")
                .arrivalStationCode("BER")
                .departureTime(departureTime)
                .arrivalTime(departureTime.plusHours(4).plusMinutes(15))
                .price(new BigDecimal("89.90"))
                .availableSeats(250)
                .platform("12")
                .durationInMinutes(255L)
                .build();

        reConnectionDTO = TrainConnectionDTO.builder()
                .id(2L)
                .trainNumber("RE 4567")
                .trainType(TrainType.RE)
                .departureStationId(1L)
                .departureStationName("Frankfurt Hbf")
                .departureStationCode("FRA")
                .arrivalStationId(2L)
                .arrivalStationName("Berlin Hbf")
                .arrivalStationCode("BER")
                .departureTime(departureTime.plusHours(1))
                .arrivalTime(departureTime.plusHours(7))
                .price(new BigDecimal("45.50"))
                .availableSeats(180)
                .platform("5")
                .durationInMinutes(360L)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/connections Tests")
    class GetAllConnectionsTests {

        @Test
        @DisplayName("sollte alle Verbindungen zurückgeben")
        void shouldReturnAllConnections() throws Exception {
            when(connectionService.findAll()).thenReturn(List.of(iceConnectionDTO, reConnectionDTO));

            mockMvc.perform(get("/api/v1/connections"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].trainNumber", is("ICE 123")))
                    .andExpect(jsonPath("$[1].trainNumber", is("RE 4567")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/connections/{id} Tests")
    class GetConnectionByIdTests {

        @Test
        @DisplayName("sollte Verbindung nach ID zurückgeben")
        void shouldReturnConnectionById() throws Exception {
            when(connectionService.findById(1L)).thenReturn(iceConnectionDTO);

            mockMvc.perform(get("/api/v1/connections/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trainNumber", is("ICE 123")))
                    .andExpect(jsonPath("$.trainType", is("ICE")))
                    .andExpect(jsonPath("$.durationInMinutes", is(255)));
        }

        @Test
        @DisplayName("sollte 404 zurückgeben wenn nicht gefunden")
        void shouldReturn404WhenNotFound() throws Exception {
            when(connectionService.findById(99L))
                    .thenThrow(new ResourceNotFoundException("Verbindung nicht gefunden"));

            mockMvc.perform(get("/api/v1/connections/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/connections/search Tests")
    class SearchConnectionsTests {

        @Test
        @DisplayName("sollte Verbindungen zwischen Bahnhöfen suchen")
        void shouldSearchConnections() throws Exception {
            when(connectionService.searchConnections(eq("FRA"), eq("BER"), any(LocalDateTime.class)))
                    .thenReturn(List.of(iceConnectionDTO, reConnectionDTO));

            mockMvc.perform(get("/api/v1/connections/search")
                            .param("from", "FRA")
                            .param("to", "BER")
                            .param("departure", "2026-02-10T08:00:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("sollte Verbindungen nach Zugtyp filtern")
        void shouldFilterByTrainType() throws Exception {
            when(connectionService.searchConnectionsByType(
                    eq("FRA"), eq("BER"), any(LocalDateTime.class), eq(TrainType.ICE)))
                    .thenReturn(List.of(iceConnectionDTO));

            mockMvc.perform(get("/api/v1/connections/search")
                            .param("from", "FRA")
                            .param("to", "BER")
                            .param("departure", "2026-02-10T08:00:00")
                            .param("type", "ICE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].trainType", is("ICE")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/connections/departures/{stationCode} Tests")
    class GetDeparturesTests {

        @Test
        @DisplayName("sollte Abfahrten von Bahnhof zurückgeben")
        void shouldReturnDeparturesFromStation() throws Exception {
            when(connectionService.findDepartures(eq("FRA"), any(LocalDateTime.class)))
                    .thenReturn(List.of(iceConnectionDTO, reConnectionDTO));

            mockMvc.perform(get("/api/v1/connections/departures/FRA")
                            .param("from", "2026-02-10T08:00:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].departureStationCode", is("FRA")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/connections/train/{trainNumber} Tests")
    class SearchByTrainNumberTests {

        @Test
        @DisplayName("sollte nach Zugnummer suchen")
        void shouldSearchByTrainNumber() throws Exception {
            when(connectionService.searchByTrainNumber("ICE"))
                    .thenReturn(List.of(iceConnectionDTO));

            mockMvc.perform(get("/api/v1/connections/train/ICE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].trainNumber", containsString("ICE")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/connections Tests")
    class CreateConnectionTests {

        @Test
        @DisplayName("sollte neue Verbindung erstellen")
        void shouldCreateConnection() throws Exception {
            when(connectionService.create(any(TrainConnectionDTO.class))).thenReturn(iceConnectionDTO);

            TrainConnectionDTO newConnection = TrainConnectionDTO.builder()
                    .trainNumber("ICE 123")
                    .trainType(TrainType.ICE)
                    .departureStationId(1L)
                    .arrivalStationId(2L)
                    .departureTime(departureTime)
                    .arrivalTime(departureTime.plusHours(4))
                    .price(new BigDecimal("89.90"))
                    .availableSeats(250)
                    .build();

            mockMvc.perform(post("/api/v1/connections")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newConnection)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.trainNumber", is("ICE 123")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/connections/{id} Tests")
    class DeleteConnectionTests {

        @Test
        @DisplayName("sollte Verbindung löschen")
        void shouldDeleteConnection() throws Exception {
            doNothing().when(connectionService).delete(1L);

            mockMvc.perform(delete("/api/v1/connections/1"))
                    .andExpect(status().isNoContent());

            verify(connectionService).delete(1L);
        }
    }
}