package com.campus.booking.repository;

import com.campus.booking.entity.Resource;
import com.campus.booking.enums.ResourceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByActiveTrue();

    List<Resource> findByTypeAndActiveTrue(ResourceType type);

    Optional<Resource> findByIdAndActiveTrue(Long id);

    @Query("SELECT r FROM Resource r WHERE (:keyword IS NULL OR " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type IS NULL OR r.type = :type) " +
           "AND (:active IS NULL OR r.active = :active)")
    List<Resource> searchResources(@Param("keyword") String keyword,
                                   @Param("type") ResourceType type,
                                   @Param("active") Boolean active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdWithLock(@Param("id") Long id);

    long countByActiveTrue();
}
