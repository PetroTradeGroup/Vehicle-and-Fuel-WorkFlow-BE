package zw.co.petrotrade.workflow.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zw.co.petrotrade.workflow.fuel.FuelType;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TransportRequestRepository extends JpaRepository<TransportRequest, Long> {

    boolean existsByFuelType(FuelType fuelType);

    // trips whose dates overlap [from, to]
    @Query("select r from TransportRequest r where r.requiredFrom <= :to and r.requiredTo >= :from "
            + "and r.status in :statuses order by r.requiredFrom, r.id")
    List<TransportRequest> findTripsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                            @Param("statuses") Collection<RequestStatus> statuses);
}
