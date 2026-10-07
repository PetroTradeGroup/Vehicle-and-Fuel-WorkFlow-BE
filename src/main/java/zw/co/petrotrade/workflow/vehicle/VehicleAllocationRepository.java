package zw.co.petrotrade.workflow.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VehicleAllocationRepository
        extends JpaRepository<VehicleAllocation, Long> {

    Optional<VehicleAllocation> findByRequestId(Long requestId);

    List<VehicleAllocation> findByRequestIdIn(Collection<Long> requestIds);
}
