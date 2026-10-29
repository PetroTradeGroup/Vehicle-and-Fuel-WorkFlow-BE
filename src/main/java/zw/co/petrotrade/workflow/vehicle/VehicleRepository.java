package zw.co.petrotrade.workflow.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zw.co.petrotrade.workflow.fuel.FuelType;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByFuelType(FuelType fuelType);

    // q matches registration, make, model or fuel type anywhere, ignoring case; "" matches all.
    // q is never null: Postgres can't type a null parameter inside lower() ("lower(bytea)")
    @Query("""
            select v from Vehicle v left join v.fuelType f
            where (:status is null or v.status = :status)
              and (lower(v.registrationNumber) like lower(concat('%', :q, '%'))
                   or lower(v.make) like lower(concat('%', :q, '%'))
                   or lower(v.model) like lower(concat('%', :q, '%'))
                   or lower(f.name) like lower(concat('%', :q, '%')))
            order by v.registrationNumber""")
    List<Vehicle> search(String q, VehicleStatus status);
}
