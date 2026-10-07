package zw.co.petrotrade.workflow.fuel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelTypeRepository extends JpaRepository<FuelType, Long> {

    boolean existsByCode(String code);
}
