package zw.co.petrotrade.workflow.vehicle;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleRequest;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleResponse;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleServiceRequest;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleServiceImp service;
    private final CurrentUser currentUser;

    // service due dates are for the fleet roles; everyone else gets the vehicle without them
    private VehicleResponse view(Vehicle vehicle) {
        VehicleResponse v = VehicleResponse.from(vehicle);
        return currentUser.has(Role.HR_ADMIN_MANAGER, Role.VEHICLE_ADMIN, Role.SYSTEM_ADMIN) ? v
                : new VehicleResponse(v.id(), v.registrationNumber(), v.make(), v.model(), v.fuelType(), v.status(),
                        v.odometerKm(), null, null, null, null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VEHICLE_ADMIN', 'SYSTEM_ADMIN')")
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        return VehicleResponse.from(service.create(request));
    }

    @GetMapping
    public List<VehicleResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VehicleStatus status) {
        List<Vehicle> found = (q == null || q.isBlank()) && status == null
                ? service.findAll()
                : service.search(q, status);
        return found.stream().map(this::view).toList();
    }

    @GetMapping("/{id}")
    public VehicleResponse get(@PathVariable Long id) {
        return view(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VEHICLE_ADMIN', 'SYSTEM_ADMIN')")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return VehicleResponse.from(service.update(id, request));
    }

    @PostMapping("/{id}/service")
    @PreAuthorize("hasAnyRole('VEHICLE_ADMIN', 'SYSTEM_ADMIN')")
    public VehicleResponse recordService(@PathVariable Long id, @Valid @RequestBody VehicleServiceRequest request) {
        return VehicleResponse.from(service.recordService(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('VEHICLE_ADMIN', 'SYSTEM_ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
