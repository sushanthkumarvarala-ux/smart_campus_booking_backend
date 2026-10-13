package com.campus.booking.dto.resource;

import com.campus.booking.enums.ResourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceAvailabilityResponse {

    private Long resourceId;
    private String resourceName;
    private ResourceType resourceType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openingTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closingTime;

    private boolean active;

    private List<BookedSlotDto> bookedSlots;

    private List<TimeSlotDto> slots;
}
