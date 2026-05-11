package com.db.navigator.controller;

import com.db.navigator.model.dto.TrainConnectionDTO;
import com.db.navigator.model.entity.TrainType;
import com.db.navigator.service.TrainConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/connections")
@RequiredArgsConstructor
@Tag(name = "Zugverbindungen", description = "API zur Suche und Verwaltung von Zugverbindungen")
public class TrainConnectionController {

    private final TrainConnectionService connectionService;

    @Operation(summary = "Alle Verbindungen abrufen", description = "Gibt eine Liste aller Zugverbindungen zurück")
    @ApiResponse(responseCode = "200", description = "Liste erfolgreich abgerufen")
    @GetMapping
    public ResponseEntity<List<TrainConnectionDTO>> getAllConnections() {
        return ResponseEntity.ok(connectionService.findAll());
    }

    @Operation(summary = "Verbindung nach ID suchen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verbindung gefunden"),
            @ApiResponse(responseCode = "404", description = "Verbindung nicht gefunden", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<TrainConnectionDTO> getConnectionById(
            @Parameter(description = "ID der Verbindung", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(connectionService.findById(id));
    }

    @Operation(
            summary = "Verbindungen suchen",
            description = "Sucht Zugverbindungen zwischen zwei Bahnhöfen ab einem bestimmten Zeitpunkt. " +
                    "Optional kann nach Zugtyp gefiltert werden."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verbindungen gefunden"),
            @ApiResponse(responseCode = "400", description = "Ungültige Parameter", content = @Content)
    })
    @GetMapping("/search")
    public ResponseEntity<List<TrainConnectionDTO>> searchConnections(
            @Parameter(description = "Abfahrtsbahnhof (Code)", example = "FRA")
            @RequestParam String from,
            @Parameter(description = "Ankunftsbahnhof (Code)", example = "BER")
            @RequestParam String to,
            @Parameter(description = "Früheste Abfahrtszeit", example = "2026-02-10T08:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departure,
            @Parameter(description = "Zugtyp (optional): ICE, IC, EC, RE, RB, S")
            @RequestParam(required = false) TrainType type) {

        if (type != null) {
            return ResponseEntity.ok(connectionService.searchConnectionsByType(from, to, departure, type));
        }
        return ResponseEntity.ok(connectionService.searchConnections(from, to, departure));
    }

    @Operation(
            summary = "Abfahrtstafel",
            description = "Zeigt alle Abfahrten von einem Bahnhof ab einem bestimmten Zeitpunkt"
    )
    @GetMapping("/departures/{stationCode}")
    public ResponseEntity<List<TrainConnectionDTO>> getDepartures(
            @Parameter(description = "Bahnhofscode", example = "FRA")
            @PathVariable String stationCode,
            @Parameter(description = "Ab Zeitpunkt (Standard: jetzt)", example = "2026-02-10T08:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from) {

        LocalDateTime departureTime = (from != null) ? from : LocalDateTime.now();
        return ResponseEntity.ok(connectionService.findDepartures(stationCode, departureTime));
    }

    @Operation(summary = "Nach Zugnummer suchen", description = "Sucht Verbindungen anhand der Zugnummer")
    @GetMapping("/train/{trainNumber}")
    public ResponseEntity<List<TrainConnectionDTO>> searchByTrainNumber(
            @Parameter(description = "Zugnummer (Teilübereinstimmung)", example = "ICE")
            @PathVariable String trainNumber) {
        return ResponseEntity.ok(connectionService.searchByTrainNumber(trainNumber));
    }

    @Operation(summary = "Neue Verbindung anlegen")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Verbindung erfolgreich erstellt"),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten", content = @Content),
            @ApiResponse(responseCode = "404", description = "Bahnhof nicht gefunden", content = @Content),
            @ApiResponse(responseCode = "409", description = "Verbindung existiert bereits", content = @Content)
    })
    @PostMapping
    public ResponseEntity<TrainConnectionDTO> createConnection(
            @Valid @RequestBody TrainConnectionDTO dto) {
        TrainConnectionDTO created = connectionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Verbindung aktualisieren")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verbindung erfolgreich aktualisiert"),
            @ApiResponse(responseCode = "404", description = "Verbindung nicht gefunden", content = @Content),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<TrainConnectionDTO> updateConnection(
            @Parameter(description = "ID der Verbindung", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody TrainConnectionDTO dto) {
        return ResponseEntity.ok(connectionService.update(id, dto));
    }

    @Operation(summary = "Verbindung löschen")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Verbindung erfolgreich gelöscht"),
            @ApiResponse(responseCode = "404", description = "Verbindung nicht gefunden", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(
            @Parameter(description = "ID der Verbindung", example = "1")
            @PathVariable Long id) {
        connectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}