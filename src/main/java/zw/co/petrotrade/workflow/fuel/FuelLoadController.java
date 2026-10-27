package zw.co.petrotrade.workflow.fuel;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.user.UserRepository;
import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// IT's queue: trips HR & Admin approved whose fuel still has to be put on the card
@RestController
@RequestMapping("/api/fuel-loads")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class FuelLoadController {

    private final FuelTransactionRepository transactions;
    private final FuelApprovalService fuelApprovalService;
    private final TransportRequestRepository requests;
    private final UserRepository users;
    private final VehicleRepository vehicles;
    private final FuelCardRepository cards;
    private final CurrentUser currentUser;

    public record Row(
            Long id, Long requestId, String destination, String driverName, String vehicleRegistration,
            String cardNumber, CardHolderType cardHolderType, Double litres, String fuelType,
            LocalDateTime approvedAt, boolean loaded, String loadedBy, LocalDateTime loadedAt) {}

    public record RowPage(List<Row> content, int page, int size, long totalElements, int totalPages) {}

    // e.g. ?loaded=false&page=0 for what's still to load, oldest first so nobody waits longest
    @GetMapping
    public RowPage list(
            @RequestParam(defaultValue = "false") boolean loaded,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Sort sort = loaded ? Sort.by(Sort.Direction.DESC, "id") : Sort.by(Sort.Direction.ASC, "id");
        Page<FuelTransaction> found = transactions.findTripFuel(
                loaded, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort));

        List<FuelTransaction> list = found.getContent();
        Map<Long, TransportRequest> requestById = byId(requests.findAllById(ids(list, FuelTransaction::getRequestId)), TransportRequest::getId);
        Map<Long, User> userById = byId(users.findAllById(ids(list, FuelTransaction::getDriverId)), User::getId);
        Map<Long, Vehicle> vehicleById = byId(vehicles.findAllById(ids(list, FuelTransaction::getVehicleId)), Vehicle::getId);
        Map<Long, FuelCard> cardById = byId(cards.findAllById(ids(list, FuelTransaction::getFuelCardId)), FuelCard::getId);

        List<Row> rows = list.stream().map(t -> {
            TransportRequest request = requestById.get(t.getRequestId());
            User driver = userById.get(t.getDriverId());
            Vehicle vehicle = vehicleById.get(t.getVehicleId());
            FuelCard card = cardById.get(t.getFuelCardId());
            return new Row(t.getId(), t.getRequestId(),
                    request == null ? null : request.getDestination(),
                    driver == null ? null : driver.getFullName(),
                    vehicle == null ? null : vehicle.getRegistrationNumber(),
                    card == null ? null : card.getCardNumber(),
                    card == null ? null : card.getHolderType(),
                    t.getAllocatedLitres(),
                    request == null || request.getFuelType() == null ? null : request.getFuelType().getName(),
                    t.getTransactionDate(),
                    !Boolean.FALSE.equals(t.getLoaded()), t.getLoadedBy(), t.getLoadedAt());
        }).toList();
        return new RowPage(rows, found.getNumber(), found.getSize(), found.getTotalElements(), found.getTotalPages());
    }

    @PostMapping("/{id}/loaded")
    public void markLoaded(@PathVariable Long id) {
        fuelApprovalService.markLoaded(id, currentUser.get().getFullName());
    }

    private static List<Long> ids(List<FuelTransaction> list, Function<FuelTransaction, Long> id) {
        return list.stream().map(id).filter(Objects::nonNull).distinct().toList();
    }

    private static <T> Map<Long, T> byId(List<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
