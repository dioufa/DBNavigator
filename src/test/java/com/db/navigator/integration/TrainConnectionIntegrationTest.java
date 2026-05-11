package com.db.navigator.integration;

import com.db.navigator.config.TestcontainersConfiguration;
import com.db.navigator.model.dto.TrainConnectionDTO;
import com.db.navigator.model.entity.Station;
import com.db.navigator.model.entity.TrainConnection;
import com.db.navigator.model.entity.TrainType;
import com.db.navigator.repository.StationRepository;
import com.db.navigator.repository.TrainConnectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TrainConnection Integration Tests")
class TrainConnectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TrainConnectionRepository connectionRepository;

    @Autowired
    private StationRepository stationRepository;

    private Station frankfurt;
    private Station berlin;
    private Station munich;

    @BeforeEach
    void setUp() {
        connectionRepository.deleteAll();
        stationRepository.deleteAll();

        frankfurt = stationRepository.save(Station.builder()
                .code("FRA").name("Frankfurt Hbf").city("Frankfurt").build());
        berlin = stationRepository.save(Station.builder()
                .code("BER").name("Berlin Hbf").city("Berlin").build());
        munich = stationRepository.save(Station.builder()
                .code("MUC").name("München Hbf").city("München").build());
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/connections - sollte neue Verbindung erstellen")
    void shouldCreateConnection() throws Exception {
        LocalDateTime departure = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        LocalDateTime arrival = departure.plusHours(4).plusMinutes(15);

        TrainConnectionDTO newConnection = TrainConnectionDTO.builder()
                .trainNumber("ICE 123")
                .trainType(TrainType.ICE)
                .departureStationId(frankfurt.getId())
                .arrivalStationId(berlin.getId())
                .departureTime(departure)
                .arrivalTime(arrival)
                .price(new BigDecimal("89.90"))
                .availableSeats(250)
                .platform("12")
                .build();

        mockMvc.perform(post("/api/v1/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConnection)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.trainNumber").value("ICE 123"))
                .andExpect(jsonPath("$.trainType").value("ICE"))
                .andExpect(jsonPath("$.departureStationName").value("Frankfurt Hbf"))
                .andExpect(jsonPath("$.arrivalStationName").value("Berlin Hbf"));

        assertThat(connectionRepository.count()).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/v1/connections - sollte alle Verbindungen zurückgeben")
    void shouldReturnAllConnections() throws Exception {
        // Given
        createTestConnection("ICE 100", TrainType.ICE, frankfurt, berlin);
        createTestConnection("RE 200", TrainType.RE, frankfurt, munich);

        // When & Then
        mockMvc.perform(get("/api/v1/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/connections/{id} - sollte Verbindung nach ID finden")
    void shouldFindConnectionById() throws Exception {
        // Given
        TrainConnection saved = createTestConnection("ICE 500", TrainType.ICE, frankfurt, berlin);

        // When & Then
        mockMvc.perform(get("/api/v1/connections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainNumber").value("ICE 500"))
                .andExpect(jsonPath("$.durationInMinutes").exists());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/connections/search - sollte Verbindungen suchen")
    void shouldSearchConnections() throws Exception {
        // Given
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(6).withMinute(0);

        createTestConnectionWithTime("ICE 601", TrainType.ICE, frankfurt, berlin,
                baseTime.withHour(8), baseTime.withHour(12));
        createTestConnectionWithTime("ICE 602", TrainType.ICE, frankfurt, berlin,
                baseTime.withHour(10), baseTime.withHour(14));
        createTestConnectionWithTime("RE 603", TrainType.RE, frankfurt, munich,
                baseTime.withHour(9), baseTime.withHour(13));

        // When & Then
        mockMvc.perform(get("/api/v1/connections/search")
                        .param("from", "FRA")
                        .param("to", "BER")
                        .param("departure", baseTime.withHour(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].trainNumber", everyItem(startsWith("ICE"))));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v1/connections/search?type=ICE - sollte nach Zugtyp filtern")
    void shouldFilterByTrainType() throws Exception {
        // Given
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(6).withMinute(0);

        createTestConnectionWithTime("ICE 701", TrainType.ICE, frankfurt, berlin,
                baseTime.withHour(8), baseTime.withHour(12));
        createTestConnectionWithTime("RE 702", TrainType.RE, frankfurt, berlin,
                baseTime.withHour(9), baseTime.withHour(15));

        // When & Then
        mockMvc.perform(get("/api/v1/connections/search")
                        .param("from", "FRA")
                        .param("to", "BER")
                        .param("departure", baseTime.withHour(7).toString())
                        .param("type", "ICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trainType").value("ICE"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/connections/departures/{code} - sollte Abfahrtstafel anzeigen")
    void shouldShowDepartureBoard() throws Exception {
        // Given
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(6).withMinute(0);

        createTestConnectionWithTime("ICE 801", TrainType.ICE, frankfurt, berlin,
                baseTime.withHour(8), baseTime.withHour(12));
        createTestConnectionWithTime("RE 802", TrainType.RE, frankfurt, munich,
                baseTime.withHour(9), baseTime.withHour(13));

        // When & Then
        mockMvc.perform(get("/api/v1/connections/departures/{code}", "FRA")
                        .param("from", baseTime.withHour(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].departureStationCode", everyItem(is("FRA"))));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/v1/connections/train/{number} - sollte nach Zugnummer suchen")
    void shouldSearchByTrainNumber() throws Exception {
        // Given
        createTestConnection("ICE 123", TrainType.ICE, frankfurt, berlin);
        createTestConnection("ICE 124", TrainType.ICE, berlin, munich);
        createTestConnection("RE 500", TrainType.RE, frankfurt, munich);

        // When & Then
        mockMvc.perform(get("/api/v1/connections/train/{number}", "ICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].trainNumber", everyItem(startsWith("ICE"))));
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/v1/connections/{id} - sollte Verbindung löschen")
    void shouldDeleteConnection() throws Exception {
        // Given
        TrainConnection saved = createTestConnection("ICE 999", TrainType.ICE, frankfurt, berlin);

        // When & Then
        mockMvc.perform(delete("/api/v1/connections/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(connectionRepository.findById(saved.getId())).isEmpty();
    }

    // Helper Methoden
    private TrainConnection createTestConnection(String trainNumber, TrainType type,
                                                 Station from, Station to) {
        LocalDateTime departure = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        return createTestConnectionWithTime(trainNumber, type, from, to,
                departure, departure.plusHours(4));
    }

    private TrainConnection createTestConnectionWithTime(String trainNumber, TrainType type,
                                                         Station from, Station to,
                                                         LocalDateTime departure,
                                                         LocalDateTime arrival) {
        return connectionRepository.save(TrainConnection.builder()
                .trainNumber(trainNumber)
                .trainType(type)
                .departureStation(from)
                .arrivalStation(to)
                .departureTime(departure)
                .arrivalTime(arrival)
                .price(new BigDecimal("59.90"))
                .availableSeats(200)
                .platform("5")
                .build());
    }
}