package zw.co.petrotrade.workflow.vehicle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.fuel.CardHolderType;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

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

    public void delete(Long id) {
        Vehicle vehicle = findById(id);
        if (vehicle.getStatus() == VehicleStatus.ALLOCATED) {
            throw new IllegalStateException("Vehicle " + id + " is currently allocated and cannot be deleted");
        }
        if (fuelCardRepository.findByHolderTypeAndHolderId(CardHolderType.VEHICLE, id).isPresent()) {
            throw new IllegalStateException("Vehicle " + id + " still has a fuel card registered to it");
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
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }
    }
}
