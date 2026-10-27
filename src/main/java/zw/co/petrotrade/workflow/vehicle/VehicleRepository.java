package zw.co.petrotrade.workflow.vehicle;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumber(String registrationNumber);

    // q matches registration, make or model anywhere, ignoring case; either filter can be null
    @Query("""
            select v from Vehicle v
            where (:status is null or v.status = :status)
              and (:q is null or lower(v.registrationNumber) like lower(concat('%', :q, '%'))
                   or lower(v.make) like lower(concat('%', :q, '%'))
                   or lower(v.model) like lower(concat('%', :q, '%')))
            order by v.registrationNumber""")
    List<Vehicle> search(String q, VehicleStatus status, Pageable pageable);
}
