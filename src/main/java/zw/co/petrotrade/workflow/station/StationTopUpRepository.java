package zw.co.petrotrade.workflow.station;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StationTopUpRepository extends JpaRepository<StationTopUp, Long> {

    List<StationTopUp> findByStationIdOrderByIdDesc(Long stationId);

    long countByStationIdAndStatus(Long stationId, TopUpStatus status);

    // requested within [from, to), any station
    List<StationTopUp> findByRequestedAtGreaterThanEqualAndRequestedAtLessThanOrderByRequestedAtAsc(
            LocalDateTime from, LocalDateTime to);

    // requested within [from, to)
    List<StationTopUp> findByStationIdAndRequestedAtGreaterThanEqualAndRequestedAtLessThanOrderByRequestedAtAsc(
            Long stationId, LocalDateTime from, LocalDateTime to);
}
