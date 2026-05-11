package com.db.navigator.service;

import com.db.navigator.exception.DuplicateResourceException;
import com.db.navigator.exception.ResourceNotFoundException;
import com.db.navigator.model.dto.StationDTO;
import com.db.navigator.model.entity.Station;
import com.db.navigator.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StationService Tests")
class StationServiceTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationService stationService;

    private Station frankfurtStation;
    private Station berlinStation;
    private StationDTO frankfurtDTO;

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

        frankfurtDTO = StationDTO.builder()
                .code("FRA")
                .name("Frankfurt Hbf")
                .city("Frankfurt")
                .build();
    }

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("sollte alle Bahnhöfe zurückgeben")
        void shouldReturnAllStations() {
            when(stationRepository.findAll()).thenReturn(List.of(frankfurtStation, berlinStation));

            List<StationDTO> result = stationService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("FRA");
            assertThat(result.get(1).getCode()).isEqualTo("BER");
            verify(stationRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("sollte leere Liste zurückgeben wenn keine Bahnhöfe existieren")
        void shouldReturnEmptyListWhenNoStations() {
            when(stationRepository.findAll()).thenReturn(List.of());

            List<StationDTO> result = stationService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("sollte Bahnhof nach ID finden")
        void shouldFindStationById() {
            when(stationRepository.findById(1L)).thenReturn(Optional.of(frankfurtStation));

            StationDTO result = stationService.findById(1L);

            assertThat(result.getCode()).isEqualTo("FRA");
            assertThat(result.getName()).isEqualTo("Frankfurt Hbf");
            assertThat(result.getCity()).isEqualTo("Frankfurt");
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Bahnhof nicht gefunden")
        void shouldThrowExceptionWhenStationNotFound() {
            when(stationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stationService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByCode Tests")
    class FindByCodeTests {

        @Test
        @DisplayName("sollte Bahnhof nach Code finden")
        void shouldFindStationByCode() {
            when(stationRepository.findByCode("FRA")).thenReturn(Optional.of(frankfurtStation));

            StationDTO result = stationService.findByCode("FRA");

            assertThat(result.getCode()).isEqualTo("FRA");
            assertThat(result.getName()).isEqualTo("Frankfurt Hbf");
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Code nicht gefunden")
        void shouldThrowExceptionWhenCodeNotFound() {
            when(stationRepository.findByCode("XXX")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stationService.findByCode("XXX"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("searchByName Tests")
    class SearchByNameTests {

        @Test
        @DisplayName("sollte Bahnhöfe nach Name suchen")
        void shouldSearchStationsByName() {
            when(stationRepository.searchByName("Frank")).thenReturn(List.of(frankfurtStation));

            List<StationDTO> result = stationService.searchByName("Frank");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).contains("Frankfurt");
        }

        @Test
        @DisplayName("sollte leere Liste bei keinem Treffer zurückgeben")
        void shouldReturnEmptyListWhenNoMatch() {
            when(stationRepository.searchByName("XYZ")).thenReturn(List.of());

            List<StationDTO> result = stationService.searchByName("XYZ");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {

        @Test
        @DisplayName("sollte neuen Bahnhof erstellen")
        void shouldCreateNewStation() {
            when(stationRepository.existsByCode("FRA")).thenReturn(false);
            when(stationRepository.save(any(Station.class))).thenReturn(frankfurtStation);

            StationDTO result = stationService.create(frankfurtDTO);

            assertThat(result.getCode()).isEqualTo("FRA");
            assertThat(result.getName()).isEqualTo("Frankfurt Hbf");
            verify(stationRepository).save(any(Station.class));
        }

        @Test
        @DisplayName("sollte Exception werfen bei doppeltem Code")
        void shouldThrowExceptionWhenCodeExists() {
            when(stationRepository.existsByCode("FRA")).thenReturn(true);

            assertThatThrownBy(() -> stationService.create(frankfurtDTO))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(stationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update Tests")
    class UpdateTests {

        @Test
        @DisplayName("sollte Bahnhof aktualisieren")
        void shouldUpdateStation() {
            StationDTO updateDTO = StationDTO.builder()
                    .code("FRA")
                    .name("Frankfurt (Main) Hbf")
                    .city("Frankfurt am Main")
                    .build();

            Station updatedStation = Station.builder()
                    .id(1L)
                    .code("FRA")
                    .name("Frankfurt (Main) Hbf")
                    .city("Frankfurt am Main")
                    .build();

            when(stationRepository.findById(1L)).thenReturn(Optional.of(frankfurtStation));
            when(stationRepository.save(any(Station.class))).thenReturn(updatedStation);

            StationDTO result = stationService.update(1L, updateDTO);

            assertThat(result.getName()).isEqualTo("Frankfurt (Main) Hbf");
            assertThat(result.getCity()).isEqualTo("Frankfurt am Main");
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Bahnhof nicht existiert")
        void shouldThrowExceptionWhenStationNotExists() {
            when(stationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stationService.update(99L, frankfurtDTO))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(stationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("sollte Bahnhof löschen")
        void shouldDeleteStation() {
            when(stationRepository.findById(1L)).thenReturn(Optional.of(frankfurtStation));
            doNothing().when(stationRepository).deleteById(1L);

            stationService.delete(1L);

            verify(stationRepository).deleteById(1L);
        }

        @Test
        @DisplayName("sollte Exception werfen wenn Bahnhof nicht existiert")
        void shouldThrowExceptionWhenStationNotExists() {
            when(stationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stationService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(stationRepository, never()).deleteById(any());
        }
    }
}