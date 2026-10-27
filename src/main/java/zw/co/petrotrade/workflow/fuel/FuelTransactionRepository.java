package zw.co.petrotrade.workflow.fuel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FuelTransactionRepository
        extends JpaRepository<FuelTransaction, Long> {

    // trip fuel only; rows from before loading was tracked (loaded is null) count as loaded
    @Query("select t from FuelTransaction t where t.requestId is not null and coalesce(t.loaded, true) = :loaded")
    Page<FuelTransaction> findTripFuel(boolean loaded, Pageable pageable);

    List<FuelTransaction> findByRequestIdIn(Collection<Long> requestIds);

    Optional<FuelTransaction> findFirstByRequestIdOrderByIdDesc(Long requestId);
}
