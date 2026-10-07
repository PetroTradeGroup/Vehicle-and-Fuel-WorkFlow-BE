package zw.co.petrotrade.workflow.fuel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FuelTransactionRepository
        extends JpaRepository<FuelTransaction, Long> {

    List<FuelTransaction> findByRequestIdIn(Collection<Long> requestIds);

    Optional<FuelTransaction> findFirstByRequestIdOrderByIdDesc(Long requestId);
}
