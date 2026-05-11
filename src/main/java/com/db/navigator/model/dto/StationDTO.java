package com.db.navigator.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDTO {

    private Long id;

    @NotBlank(message = "Code ist erforderlich")
    @Size(min = 2, max = 10, message = "Code muss zwischen 2 und 10 Zeichen sein")
    private String code;

    @NotBlank(message = "Name ist erforderlich")
    private String name;

    @NotBlank(message = "Stadt ist erforderlich")
    private String city;
}
