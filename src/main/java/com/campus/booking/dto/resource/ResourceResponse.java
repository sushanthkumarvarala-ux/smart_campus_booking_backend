package com.campus.booking.dto.resource;

import com.campus.booking.entity.Resource;
import com.campus.booking.enums.ResourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceResponse {

    private Long id;
    private String name;
    private ResourceType type;
    private String location;
    private Integer capacity;
    private String description;
    private boolean active;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openingTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closingTime;

    private LocalDateTime createdAt;

    public static ResourceResponse fromEntity(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .type(resource.getType())
                .location(resource.getLocation())
                .capacity(resource.getCapacity())
                .description(resource.getDescription())
                .active(resource.isActive())
                .openingTime(resource.getOpeningTime())
                .closingTime(resource.getClosingTime())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
