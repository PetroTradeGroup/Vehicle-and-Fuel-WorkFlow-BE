package zw.co.petrotrade.workflow.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.fuel.FuelCalculationService;
import zw.co.petrotrade.workflow.fuel.NotificationService;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.user.UserRepository;

@Service
@RequiredArgsConstructor
public class TransportRequestService {

    private final TransportRequestRepository repository;
    private final UserRepository userRepository;
    private final FuelCalculationService fuelCalculationService;
    private final NotificationService notificationService;

    public TransportRequest create(TransportRequest request) {

        User driver = userRepository.findById(request.getDriverId()).orElseThrow();
        request.setDriverName(driver.getFullName());
        request.setDepartment(driver.getDepartment());
        request.setJobTitle(driver.getJobTitle());
        request.setLicenceNumber(driver.getLicenceNumber());

        request.setFuelRequiredLitres(fuelCalculationService.calculateFuel(request.getDistanceKm()));
        request.setStatus(RequestStatus.PENDING_HOD);

        return repository.save(request);
    }

    public TransportRequest calculateFuel(Long requestId) {

        TransportRequest request = repository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.VEHICLE_ALLOCATED) {
            throw new IllegalStateException(
                    "Request " + requestId + " does not have an allocated vehicle yet (current status: "
                            + request.getStatus() + ")");
        }

        request.setFuelRequiredLitres(
                fuelCalculationService.calculateFuel(request.getDistanceKm()));
        request.setStatus(RequestStatus.FUEL_CALCULATED);

        repository.save(request);

        notificationService.notifyFuelRequestSubmitted(request);

        return request;
    }
}
