package zw.co.petrotrade.workflow.fuel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.fuel.dto.FuelTypeRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FuelTypeServiceImpl implements FuelTypeService {

    private final FuelTypeRepository repository;
    private final TransportRequestRepository transportRequestRepository;

    public FuelType create(FuelTypeRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new IllegalStateException("Fuel type " + request.code() + " already exists");
        }
        FuelType fuelType = new FuelType();
        apply(fuelType, request);
        return repository.save(fuelType);
    }

    public List<FuelType> findAll() {
        return repository.findAll();
    }

    public FuelType findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public FuelType update(Long id, FuelTypeRequest request) {
        FuelType fuelType = findById(id);
        if (!fuelType.getCode().equals(request.code()) && repository.existsByCode(request.code())) {
            throw new IllegalStateException("Fuel type " + request.code() + " already exists");
        }
        apply(fuelType, request);
        return repository.save(fuelType);
    }

    public void delete(Long id) {
        FuelType fuelType = findById(id);
        if (transportRequestRepository.existsByFuelType(fuelType)) {
            throw new IllegalStateException("Fuel type " + fuelType.getCode() + " is used by transport requests");
        }
        repository.delete(fuelType);
    }

    private void apply(FuelType fuelType, FuelTypeRequest request) {
        fuelType.setName(request.name());
        fuelType.setCode(request.code());
    }
}
