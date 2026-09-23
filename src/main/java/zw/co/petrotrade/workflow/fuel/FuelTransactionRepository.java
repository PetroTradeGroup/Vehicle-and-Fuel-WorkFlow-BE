package zw.co.petrotrade.workflow.fuel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelTransactionRepository
        extends JpaRepository<FuelTransaction, Long> {
}