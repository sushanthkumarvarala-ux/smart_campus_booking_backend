package com.campus.booking.dto.resource;

import com.campus.booking.enums.ResourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 150, message = "Resource name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Resource type is required")
    private ResourceType type;

    @NotBlank(message = "Location is required")
    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private String description;

    @Builder.Default
    private Boolean active = true;

    @NotNull(message = "Opening time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openingTime;

    @NotNull(message = "Closing time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closingTime;
}
