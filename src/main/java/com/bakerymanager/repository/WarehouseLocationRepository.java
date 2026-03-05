package com.bakerymanager.repository;

import com.bakerymanager.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {

    Optional<WarehouseLocation> findByLocationCode(String locationCode);

    List<WarehouseLocation> findByActiveTrueOrderByWarehouseNameAscZoneNameAsc();
}
