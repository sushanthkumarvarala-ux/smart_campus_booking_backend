package com.campus.booking.controller;

import com.campus.booking.dto.resource.ResourceAvailabilityResponse;
import com.campus.booking.dto.resource.ResourceResponse;
import com.campus.booking.enums.ResourceType;
import com.campus.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Public & authenticated endpoints to browse resources and view availability")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "Browse resources with search keyword and type filters")
    public ResponseEntity<List<ResourceResponse>> getAllResources(
            @Parameter(description = "Keyword to search in name, location, or description")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by resource category type")
            @RequestParam(required = false) ResourceType type,
            @Parameter(description = "Filter by active status (defaults to active for public browsing)")
            @RequestParam(required = false, defaultValue = "true") Boolean active) {

        List<ResourceResponse> resources = resourceService.getAllResources(keyword, type, active);
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a specific resource")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        ResourceResponse resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Check booked intervals and hourly slot availability for a resource on a specific date")
    public ResponseEntity<ResourceAvailabilityResponse> getResourceAvailability(
            @PathVariable Long id,
            @Parameter(description = "Date in format YYYY-MM-DD (defaults to today if omitted)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        ResourceAvailabilityResponse availability = resourceService.getResourceAvailability(id, date);
        return ResponseEntity.ok(availability);
    }
}
