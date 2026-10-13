package com.campus.booking.service;

import com.campus.booking.dto.resource.ResourceAvailabilityResponse;
import com.campus.booking.dto.resource.ResourceRequest;
import com.campus.booking.dto.resource.ResourceResponse;
import com.campus.booking.enums.ResourceType;

import java.time.LocalDate;
import java.util.List;

public interface ResourceService {

    List<ResourceResponse> getAllResources(String keyword, ResourceType type, Boolean active);

    ResourceResponse getResourceById(Long id);

    ResourceAvailabilityResponse getResourceAvailability(Long id, LocalDate date);

    ResourceResponse createResource(ResourceRequest request);

    ResourceResponse updateResource(Long id, ResourceRequest request);

    void deleteResource(Long id);
}
