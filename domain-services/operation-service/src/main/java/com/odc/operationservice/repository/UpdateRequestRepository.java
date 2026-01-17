package com.odc.operationservice.repository;

import com.odc.operationservice.entity.UpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UpdateRequestRepository extends JpaRepository<UpdateRequest, UUID>, JpaSpecificationExecutor<UpdateRequest> {

    @Query(
            value = "SELECT LPAD(CAST(nextval('update_request_code_seq') AS text), 8, '0')",
            nativeQuery = true
    )
    String generateNextCode();

}

