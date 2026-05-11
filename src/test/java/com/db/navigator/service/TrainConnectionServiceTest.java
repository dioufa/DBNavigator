package com.db.navigator.service;

import com.db.navigator.exception.DuplicateResourceException;
import com.db.navigator.exception.ResourceNotFoundException;
import com.db.navigator.model.dto.TrainConnectionDTO;
import com.db.navigator.model.entity.Station;
import com.db.navigator.model.entity.TrainConnection;
import com.db.navigator.model.entity.TrainType;
import com.db.navigator.repository.StationRepository;
import com.db.navigator.repository.TrainConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainConnectionService Tests")
class TrainConnectionServiceTest {

    @Mock
    private TrainConnectionRepository connectionRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private TrainConnectionService connectionService;

    private Station frankfurtStation;
    private Station berlinStation;
    private TrainConnection iceConnection;
    private TrainConnectionDTO connectionDTO;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    @BeforeEach
    void setUp() {
        frankfurtStation = Station.builder()
                .id(1L)
                .code("FRA")
                .name("Frankfurt Hbf")
                .city("Frankfurt")
                .build();

        berlinStation = Station.builder()
                .id(2L)
                .code("BER")
                .name("Berlin Hbf")
                .city("Berlin")
                .build();

        departureTime = LocalDateTime.of(2026, 2, 10, 8, 0);
        arrivalTime = LocalDateTime.of(2026, 2, 10, 12, 15);

        iceConnection = TrainConnection.builder()
                .id(1L)
                .trainNumber("ICE 123")
                .trainType(TrainType.ICE)
                .departureStation(frankfurtStation)
                .arrivalStation(berlinStation)
                .departureTime(departureTime)
                .arrivalTime(arrivalTime)
                .price(new BigDecimal("89.90"))
                .availableSeats(250)
                .platform("12")
                .build();

        connectionDTO = TrainConnectionDTO.builder()
                .trainNumber("ICE 123")
                .trainType(TrainType.ICE)
                .departureStationId(1L)
                .arrivalStationId(2L)
                .departureTime(departureTime)
                .arrivalTime(arrivalTime)
                .price(new BigDecimal("89.90"))
                .availableSeats(250)
                .platform("12")
                .build();
    }

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("sollte alle Verbindungen zurückgeben")
        void shouldReturnAllConnections() {
            when(connectionRepository.findAll()).thenReturn(List.of(iceConnection));

            List<TrainConnectionDTO> result = connectionService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTrainNumber()).isEqualTo("ICE 123");
            assertThat(result.get(0).getTrainType()).isEqualTo(TrainType.ICE);
        }

        @Test
        @DisplayName("sollte leere Liste zurückgeben wenn keine Verbindungen")
        void shouldReturnEmptyListWhenNoConnections() {
            when(connectionRepository.findAll()).thenReturn(List.of());

            List<TrainConnectionDTO> result = connectionService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("sollte Verbindung nach ID finden")
        void shouldFindConnectionById() {
            when(connectionRepository.findById(1L)).thenReturn(Optional.of(iceConnection));

            TrainConnectionDTO result = connectionService.findById(1L);

            assertThat(result.getTrainNumber()).isEqualTo("ICE 123");
            assertThat(result.getDepartureStationName()).isEqualTo("Frankfurt Hbf");
            assertThat(result.getArrivalStationName()).isEqualTo("Berlin Hbf");
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Verbindung nicht gefunden")
        void shouldThrowExceptionWhenConnectionNotFound() {
            when(connectionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("searchConnections Tests")
    class SearchConnectionsTests {

        @Test
        @DisplayName("sollte Verbindungen zwischen zwei Bahnhöfen finden")
        void shouldFindConnectionsBetweenStations() {
            when(connectionRepository.findConnections("FRA", "BER", departureTime))
                    .thenReturn(List.of(iceConnection));

            List<TrainConnectionDTO> result = connectionService.searchConnections("FRA", "BER", departureTime);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartureStationCode()).isEqualTo("FRA");
            assertThat(result.get(0).getArrivalStationCode()).isEqualTo("BER");
        }

        @Test
        @DisplayName("sollte leere Liste bei keinen Verbindungen zurückgeben")
        void shouldReturnEmptyListWhenNoConnections() {
            when(connectionRepository.findConnections("FRA", "MUC", departureTime))
                    .thenReturn(List.of());

            List<TrainConnectionDTO> result = connectionService.searchConnections("FRA", "MUC", departureTime);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchConnectionsByType Tests")
    class SearchConnectionsByTypeTests {

        @Test
        @DisplayName("sollte Verbindungen nach Zugtyp filtern")
        void shouldFilterConnectionsByTrainType() {
            when(connectionRepository.findConnectionsByType("FRA", "BER", departureTime, TrainType.ICE))
                    .thenReturn(List.of(iceConnection));

            List<TrainConnectionDTO> result = connectionService.searchConnectionsByType(
                    "FRA", "BER", departureTime, TrainType.ICE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTrainType()).isEqualTo(TrainType.ICE);
        }
    }

    @Nested
    @DisplayName("findDepartures Tests")
    class FindDeparturesTests {

        @Test
        @DisplayName("sollte Abfahrten von einem Bahnhof finden")
        void shouldFindDeparturesFromStation() {
            when(connectionRepository.findDeparturesFromStation("FRA", departureTime))
                    .thenReturn(List.of(iceConnection));

            List<TrainConnectionDTO> result = connectionService.findDepartures("FRA", departureTime);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartureStationCode()).isEqualTo("FRA");
        }
    }

    @Nested
    @DisplayName("searchByTrainNumber Tests")
    class SearchByTrainNumberTests {

        @Test
        @DisplayName("sollte Verbindungen nach Zugnummer suchen")
        void shouldSearchByTrainNumber() {
            when(connectionRepository.findByTrainNumberContainingIgnoreCase("ICE"))
                    .thenReturn(List.of(iceConnection));

            List<TrainConnectionDTO> result = connectionService.searchByTrainNumber("ICE");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTrainNumber()).contains("ICE");
        }
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {

        @Test
        @DisplayName("sollte neue Verbindung erstellen")
        void shouldCreateNewConnection() {
            when(connectionRepository.existsByTrainNumberAndDepartureTime("ICE 123", departureTime))
                    .thenReturn(false);
            when(stationRepository.findById(1L)).thenReturn(Optional.of(frankfurtStation));
            when(stationRepository.findById(2L)).thenReturn(Optional.of(berlinStation));
            when(connectionRepository.save(any(TrainConnection.class))).thenReturn(iceConnection);

            TrainConnectionDTO result = connectionService.create(connectionDTO);

            assertThat(result.getTrainNumber()).isEqualTo("ICE 123");
            assertThat(result.getDepartureStationName()).isEqualTo("Frankfurt Hbf");
            verify(connectionRepository).save(any(TrainConnection.class));
        }

        @Test
        @DisplayName("sollte Exception werfen bei doppelter Verbindung")
        void shouldThrowExceptionWhenConnectionExists() {
            when(connectionRepository.existsByTrainNumberAndDepartureTime("ICE 123", departureTime))
                    .thenReturn(true);

            assertThatThrownBy(() -> connectionService.create(connectionDTO))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("ICE 123");

            verify(connectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Abfahrtsbahnhof nicht existiert")
        void shouldThrowExceptionWhenDepartureStationNotFound() {
            when(connectionRepository.existsByTrainNumberAndDepartureTime("ICE 123", departureTime))
                    .thenReturn(false);
            when(stationRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.create(connectionDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Abfahrtsbahnhof");
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Ankunftsbahnhof nicht existiert")
        void shouldThrowExceptionWhenArrivalStationNotFound() {
            when(connectionRepository.existsByTrainNumberAndDepartureTime("ICE 123", departureTime))
                    .thenReturn(false);
            when(stationRepository.findById(1L)).thenReturn(Optional.of(frankfurtStation));
            when(stationRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.create(connectionDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ankunftsbahnhof");
        }
    }

    @Nested
    @DisplayName("update Tests")
    class UpdateTests {

        @Test
        @DisplayName("sollte Verbindung aktualisieren")
        void shouldUpdateConnection() {
            TrainConnectionDTO updateDTO = TrainConnectionDTO.builder()
                    .trainNumber("ICE 123")
                    .trainType(TrainType.ICE)
                    .departureStationId(1L)
                    .arrivalStationId(2L)
                    .departureTime(departureTime)
                    .arrivalTime(arrivalTime)
                    .price(new BigDecimal("99.90"))
                    .availableSeats(200)
                    .platform("10")
                    .build();

            when(connectionRepository.findById(1L)).thenReturn(Optional.of(iceConnection));
            when(stationRepository.findById(1L)).thenReturn(Optional.of(frankfurtStation));
            when(stationRepository.findById(2L)).thenReturn(Optional.of(berlinStation));
            when(connectionRepository.save(any(TrainConnection.class))).thenReturn(iceConnection);

            TrainConnectionDTO result = connectionService.update(1L, updateDTO);

            assertThat(result).isNotNull();
            verify(connectionRepository).save(any(TrainConnection.class));
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Verbindung nicht existiert")
        void shouldThrowExceptionWhenConnectionNotExists() {
            when(connectionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> connectionService.update(99L, connectionDTO))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(connectionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("sollte Verbindung löschen")
        void shouldDeleteConnection() {
            when(connectionRepository.existsById(1L)).thenReturn(true);
            doNothing().when(connectionRepository).deleteById(1L);

            connectionService.delete(1L);

            verify(connectionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Verbindung nicht existiert")
        void shouldThrowExceptionWhenConnectionNotExists() {
            when(connectionRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> connectionService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(connectionRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Berechnungen Tests")
    class CalculationTests {

        @Test
        @DisplayName("sollte Reisedauer korrekt berechnen")
        void shouldCalculateDurationCorrectly() {
            when(connectionRepository.findById(1L)).thenReturn(Optional.of(iceConnection));

            TrainConnectionDTO result = connectionService.findById(1L);

            // 8:00 bis 12:15 = 4h 15min = 255 Minuten
            assertThat(result.getDurationInMinutes()).isEqualTo(255L);
        }
    }
}