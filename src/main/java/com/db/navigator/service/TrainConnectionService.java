package com.db.navigator.service;

import com.db.navigator.exception.DuplicateResourceException;
import com.db.navigator.exception.ResourceNotFoundException;
import com.db.navigator.model.dto.TrainConnectionDTO;
import com.db.navigator.model.entity.Station;
import com.db.navigator.model.entity.TrainConnection;
import com.db.navigator.model.entity.TrainType;
import com.db.navigator.repository.StationRepository;
import com.db.navigator.repository.TrainConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainConnectionService {

    private final TrainConnectionRepository connectionRepository;
    private final StationRepository stationRepository;

    public List<TrainConnectionDTO> findAll() {
        return connectionRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public TrainConnectionDTO findById(Long id) {
        return connectionRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Verbindung nicht gefunden mit ID: " + id));
    }

    public List<TrainConnectionDTO> searchConnections(String fromCode, String toCode, LocalDateTime departure) {
        return connectionRepository.findConnections(fromCode, toCode, departure).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TrainConnectionDTO> searchConnectionsByType(
            String fromCode, String toCode, LocalDateTime departure, TrainType trainType) {
        return connectionRepository.findConnectionsByType(fromCode, toCode, departure, trainType).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TrainConnectionDTO> findDepartures(String stationCode, LocalDateTime fromTime) {
        return connectionRepository.findDeparturesFromStation(stationCode, fromTime).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TrainConnectionDTO> findAvailableConnections(
            String fromCode, String toCode, LocalDateTime departure, int minSeats) {
        return connectionRepository.findAvailableConnections(fromCode, toCode, departure, minSeats).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TrainConnectionDTO> searchByTrainNumber(String trainNumber) {
        return connectionRepository.findByTrainNumberContainingIgnoreCase(trainNumber).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public TrainConnectionDTO create(TrainConnectionDTO dto) {
        // Prüfen ob Verbindung bereits existiert
        if (connectionRepository.existsByTrainNumberAndDepartureTime(dto.getTrainNumber(), dto.getDepartureTime())) {
            throw new DuplicateResourceException(
                    "Verbindung " + dto.getTrainNumber() + " um " + dto.getDepartureTime() + " existiert bereits");
        }

        // Bahnhöfe laden
        Station departureStation = stationRepository.findById(dto.getDepartureStationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Abfahrtsbahnhof nicht gefunden mit ID: " + dto.getDepartureStationId()));

        Station arrivalStation = stationRepository.findById(dto.getArrivalStationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ankunftsbahnhof nicht gefunden mit ID: " + dto.getArrivalStationId()));

        TrainConnection connection = toEntity(dto, departureStation, arrivalStation);
        TrainConnection saved = connectionRepository.save(connection);
        return toDTO(saved);
    }

    @Transactional
    public TrainConnectionDTO update(Long id, TrainConnectionDTO dto) {
        TrainConnection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verbindung nicht gefunden mit ID: " + id));

        // Bahnhöfe laden falls geändert
        Station departureStation = stationRepository.findById(dto.getDepartureStationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Abfahrtsbahnhof nicht gefunden mit ID: " + dto.getDepartureStationId()));

        Station arrivalStation = stationRepository.findById(dto.getArrivalStationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ankunftsbahnhof nicht gefunden mit ID: " + dto.getArrivalStationId()));

        connection.setTrainNumber(dto.getTrainNumber());
        connection.setTrainType(dto.getTrainType());
        connection.setDepartureStation(departureStation);
        connection.setArrivalStation(arrivalStation);
        connection.setDepartureTime(dto.getDepartureTime());
        connection.setArrivalTime(dto.getArrivalTime());
        connection.setPrice(dto.getPrice());
        connection.setAvailableSeats(dto.getAvailableSeats());
        connection.setPlatform(dto.getPlatform());

        TrainConnection updated = connectionRepository.save(connection);
        return toDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!connectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Verbindung nicht gefunden mit ID: " + id);
        }
        connectionRepository.deleteById(id);
    }

    // Mapper Methoden
    private TrainConnectionDTO toDTO(TrainConnection connection) {
        return TrainConnectionDTO.builder()
                .id(connection.getId())
                .trainNumber(connection.getTrainNumber())
                .trainType(connection.getTrainType())
                .departureStationId(connection.getDepartureStation().getId())
                .departureStationName(connection.getDepartureStation().getName())
                .departureStationCode(connection.getDepartureStation().getCode())
                .arrivalStationId(connection.getArrivalStation().getId())
                .arrivalStationName(connection.getArrivalStation().getName())
                .arrivalStationCode(connection.getArrivalStation().getCode())
                .departureTime(connection.getDepartureTime())
                .arrivalTime(connection.getArrivalTime())
                .price(connection.getPrice())
                .availableSeats(connection.getAvailableSeats())
                .platform(connection.getPlatform())
                .durationInMinutes(connection.getDurationInMinutes())
                .build();
    }

    private TrainConnection toEntity(TrainConnectionDTO dto, Station departureStation, Station arrivalStation) {
        return TrainConnection.builder()
                .trainNumber(dto.getTrainNumber())
                .trainType(dto.getTrainType())
                .departureStation(departureStation)
                .arrivalStation(arrivalStation)
                .departureTime(dto.getDepartureTime())
                .arrivalTime(dto.getArrivalTime())
                .price(dto.getPrice())
                .availableSeats(dto.getAvailableSeats())
                .platform(dto.getPlatform())
                .build();
    }
}
