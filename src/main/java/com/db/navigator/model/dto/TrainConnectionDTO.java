package com.db.navigator.model.dto;

import com.db.navigator.model.entity.TrainType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainConnectionDTO {

    private Long id;

    @NotBlank(message = "Zugnummer ist erforderlich")
    private String trainNumber;

    @NotNull(message = "Zugtyp ist erforderlich")
    private TrainType trainType;

    @NotNull(message = "Abfahrtsbahnhof ist erforderlich")
    private Long departureStationId;

    private String departureStationName;
    private String departureStationCode;

    @NotNull(message = "Ankunftsbahnhof ist erforderlich")
    private Long arrivalStationId;

    private String arrivalStationName;
    private String arrivalStationCode;

    @NotNull(message = "Abfahrtszeit ist erforderlich")
    private LocalDateTime departureTime;

    @NotNull(message = "Ankunftszeit ist erforderlich")
    private LocalDateTime arrivalTime;

    @Positive(message = "Preis muss positiv sein")
    private BigDecimal price;

    @Positive(message = "Verfügbare Plätze müssen positiv sein")
    private Integer availableSeats;

    private String platform;

    private Long durationInMinutes;
}