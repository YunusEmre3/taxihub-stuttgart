package com.stuttgarttaxi.taxihub.repository;

import com.stuttgarttaxi.taxihub.entity.ExtraService;
import com.stuttgarttaxi.taxihub.entity.ExtraServiceCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExtraServiceRepository extends JpaRepository<ExtraService, Long> {

    Optional<ExtraService> findByCode(ExtraServiceCode code);

    List<ExtraService> findAllByOrderByIdAsc();
}
