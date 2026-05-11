package com.db.navigator.controller;

import com.db.navigator.model.dto.StationDTO;
import com.db.navigator.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
@Tag(name = "Bahnhöfe", description = "API zur Verwaltung von Bahnhöfen")
public class StationController {

    private final StationService stationService;

    @Operation(summary = "Alle Bahnhöfe abrufen", description = "Gibt eine Liste aller Bahnhöfe zurück")
    @ApiResponse(responseCode = "200", description = "Liste erfolgreich abgerufen")
    @GetMapping
    public ResponseEntity<List<StationDTO>> getAllStation() {
        return ResponseEntity.ok(stationService.findAll());
    }

    @Operation(summary = "Bahnhof nach ID suchen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bahnhof gefunden"),
            @ApiResponse(responseCode = "404", description = "Bahnhof nicht gefunden", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<StationDTO> getStationById(
            @Parameter(description = "ID des Bahnhofs", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(stationService.findById(id));
    }

    @Operation(summary = "Bahnhof nach Code suchen", description = "Sucht einen Bahnhof anhand seines Codes (z.B. FRA, BER)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bahnhof gefunden"),
            @ApiResponse(responseCode = "404", description = "Bahnhof nicht gefunden", content = @Content)
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<StationDTO> getStationByCode(
            @Parameter(description = "Bahnhofscode", example = "FRA")
            @PathVariable String code) {
        return ResponseEntity.ok(stationService.findByCode(code));
    }

    @Operation(summary = "Bahnhöfe nach Name suchen", description = "Durchsucht Bahnhofsnamen (Teilübereinstimmung)")
    @GetMapping("/search")
    public ResponseEntity<List<StationDTO>> searchStations(
            @Parameter(description = "Suchbegriff für Bahnhofsname", example = "Frank")
            @RequestParam String name) {
        return ResponseEntity.ok(stationService.searchByName(name));
    }

    @Operation(summary = "Neuen Bahnhof anlegen")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bahnhof erfolgreich erstellt"),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten", content = @Content),
            @ApiResponse(responseCode = "409", description = "Bahnhof mit diesem Code existiert bereits", content = @Content)
    })
    @PostMapping
    public ResponseEntity<StationDTO> createStation(
            @Valid @RequestBody StationDTO stationDTO) {
        StationDTO created = stationService.create(stationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Bahnhof aktualisieren")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bahnhof erfolgreich aktualisiert"),
            @ApiResponse(responseCode = "404", description = "Bahnhof nicht gefunden", content = @Content),
            @ApiResponse(responseCode = "400", description = "Ungültige Eingabedaten", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<StationDTO> updateStation(
            @Parameter(description = "ID des Bahnhofs", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody StationDTO stationDTO) {
        return ResponseEntity.ok(stationService.update(id, stationDTO));
    }

    @Operation(summary = "Bahnhof löschen")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bahnhof erfolgreich gelöscht"),
            @ApiResponse(responseCode = "404", description = "Bahnhof nicht gefunden", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(
            @Parameter(description = "ID des Bahnhofs", example = "1")
            @PathVariable Long id) {
        stationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}