package com.campus.booking.controller;

import com.campus.booking.dto.common.ApiResponse;
import com.campus.booking.dto.resource.ResourceRequest;
import com.campus.booking.dto.resource.ResourceResponse;
import com.campus.booking.enums.ResourceType;
import com.campus.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/resources")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Resources", description = "Admin-only endpoints to manage campus resources (CRUD)")
public class AdminResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "Get all resources for administrative management (active and inactive)")
    public ResponseEntity<List<ResourceResponse>> getAllAdminResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) Boolean active) {

        List<ResourceResponse> resources = resourceService.getAllResources(keyword, type, active);
        return ResponseEntity.ok(resources);
    }

    @PostMapping
    @Operation(summary = "Create a new campus resource")
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody ResourceRequest request) {
        ResourceResponse created = resourceService.createResource(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing campus resource")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {

        ResourceResponse updated = resourceService.updateResource(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete or deactivate a campus resource")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(ApiResponse.message("Resource removed or deactivated successfully"));
    }
}
