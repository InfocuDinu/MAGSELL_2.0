package com.bakerymanager.repository;

import com.bakerymanager.entity.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {

    Optional<UnitConversion> findByFromUnitAndToUnit(String fromUnit, String toUnit);
}
