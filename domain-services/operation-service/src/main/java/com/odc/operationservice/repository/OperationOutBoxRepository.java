package com.odc.operationservice.repository;

import com.odc.operationservice.entity.OperationOutBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OperationOutBoxRepository extends JpaRepository<OperationOutBox, UUID> {
    List<OperationOutBox> findTop50ByProcessedFalseOrderByCreatedAtAsc();

}
