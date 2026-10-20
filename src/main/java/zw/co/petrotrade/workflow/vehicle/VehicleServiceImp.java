package zw.co.petrotrade.workflow.vehicle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.fuel.CardHolderType;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleRequest;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleServiceRequest;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImp implements VehicleService {

    private final VehicleRepository repository;
    private final FuelCardRepository fuelCardRepository;

    public Vehicle create(VehicleRequest request) {
        if (repository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new IllegalStateException(
                    "Vehicle " + request.registrationNumber() + " is already registered");
        }
        Vehicle vehicle = new Vehicle();
        apply(vehicle, request);
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
        return repository.save(vehicle);
    }

    public List<Vehicle> findAll() {
        return repository.findAll();
    }

    public Vehicle findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Vehicle update(Long id, VehicleRequest request) {
        Vehicle vehicle = findById(id);
        if (!vehicle.getRegistrationNumber().equals(request.registrationNumber())
                && repository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new IllegalStateException(
                    "Vehicle " + request.registrationNumber() + " is already registered");
        }
        apply(vehicle, request);
        return repository.save(vehicle);
    }

    // starts a new service interval from this reading
    public Vehicle recordService(Long id, VehicleServiceRequest request) {
        Vehicle vehicle = findById(id);
        Double reading = request.odometerKm() != null ? request.odometerKm() : vehicle.getOdometerKm();
        if (reading == null) {
            throw new IllegalArgumentException("Vehicle " + vehicle.getRegistrationNumber()
                    + " has no odometer reading yet. Enter the reading at the service.");
        }
        if (vehicle.getOdometerKm() != null && reading < vehicle.getOdometerKm()) {
            throw new IllegalArgumentException(String.format(
                    "The odometer can't read less than %.0f km, the last reading for %s.",
                    vehicle.getOdometerKm(), vehicle.getRegistrationNumber()));
        }
        vehicle.setOdometerKm(reading);
        vehicle.setLastServiceOdometerKm(reading);
        vehicle.setLastServiceDate(request.serviceDate() != null ? request.serviceDate() : LocalDate.now());
        return repository.save(vehicle);
    }

    public void delete(Long id) {
        Vehicle vehicle = findById(id);
        if (vehicle.getStatus() == VehicleStatus.ALLOCATED) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber()
                    + " is out on a trip, so it can't be deleted until it's returned.");
        }
        if (fuelCardRepository.findByHolderTypeAndHolderId(CardHolderType.VEHICLE, id).isPresent()) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber()
                    + " has a fuel card registered to it, so it can't be deleted.");
        }
        repository.delete(vehicle);
    }

    private void apply(Vehicle vehicle, VehicleRequest request) {
        if (request.status() == VehicleStatus.ALLOCATED) {
            throw new IllegalArgumentException("Vehicles are only set to ALLOCATED through a vehicle allocation");
        }
        vehicle.setRegistrationNumber(request.registrationNumber());
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        if (request.odometerKm() != null) {
            vehicle.setOdometerKm(request.odometerKm());
        }
        if (request.lastServiceOdometerKm() != null) {
            if (vehicle.getOdometerKm() != null && request.lastServiceOdometerKm() > vehicle.getOdometerKm()) {
                throw new IllegalArgumentException("The last service reading can't be more than the current odometer.");
            }
            vehicle.setLastServiceOdometerKm(request.lastServiceOdometerKm());
        }
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }
    }
}
