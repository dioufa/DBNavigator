package com.db.navigator.repository;

import com.db.navigator.model.entity.TrainConnection;
import com.db.navigator.model.entity.TrainType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainConnectionRepository extends JpaRepository<TrainConnection, Long> {

    // Verbindungen zwischen zwei Bahnhöfen suchen
    @Query("SELECT c FROM TrainConnection c " +
            "JOIN FETCH c.departureStation " +
            "JOIN FETCH c.arrivalStation " +
            "WHERE c.departureStation.code = :fromCode " +
            "AND c.arrivalStation.code = :toCode " +
            "AND c.departureTime >= :minDeparture " +
            "ORDER BY c.departureTime")
    List<TrainConnection> findConnections(
            @Param("fromCode") String fromCode,
            @Param("toCode") String toCode,
            @Param("minDeparture") LocalDateTime minDeparture);

    // Verbindungen nach Zugtyp filtern
    @Query("SELECT c FROM TrainConnection c " +
            "JOIN FETCH c.departureStation " +
            "JOIN FETCH c.arrivalStation " +
            "WHERE c.departureStation.code = :fromCode " +
            "AND c.arrivalStation.code = :toCode " +
            "AND c.departureTime >= :minDeparture " +
            "AND c.trainType = :trainType " +
            "ORDER BY c.departureTime")
    List<TrainConnection> findConnectionsByType(
            @Param("fromCode") String fromCode,
            @Param("toCode") String toCode,
            @Param("minDeparture") LocalDateTime minDeparture,
            @Param("trainType") TrainType trainType);

    // Alle Verbindungen ab einem Bahnhof
    @Query("SELECT c FROM TrainConnection c " +
            "JOIN FETCH c.departureStation " +
            "JOIN FETCH c.arrivalStation " +
            "WHERE c.departureStation.code = :stationCode " +
            "AND c.departureTime >= :minDeparture " +
            "ORDER BY c.departureTime")
    List<TrainConnection> findDeparturesFromStation(
            @Param("stationCode") String stationCode,
            @Param("minDeparture") LocalDateTime minDeparture);

    // Verbindungen mit verfügbaren Plätzen
    @Query("SELECT c FROM TrainConnection c " +
            "JOIN FETCH c.departureStation " +
            "JOIN FETCH c.arrivalStation " +
            "WHERE c.departureStation.code = :fromCode " +
            "AND c.arrivalStation.code = :toCode " +
            "AND c.departureTime >= :minDeparture " +
            "AND c.availableSeats >= :minSeats " +
            "ORDER BY c.departureTime")
    List<TrainConnection> findAvailableConnections(
            @Param("fromCode") String fromCode,
            @Param("toCode") String toCode,
            @Param("minDeparture") LocalDateTime minDeparture,
            @Param("minSeats") int minSeats);

    // Nach Zugnummer suchen
    List<TrainConnection> findByTrainNumberContainingIgnoreCase(String trainNumber);

    // Prüfen ob Zugnummer zu bestimmter Zeit existiert
    boolean existsByTrainNumberAndDepartureTime(String trainNumber, LocalDateTime departureTime);
}