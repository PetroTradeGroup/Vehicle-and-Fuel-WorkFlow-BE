package zw.co.petrotrade.workflow.fuel;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.fuel.dto.FuelTypeRequest;
import zw.co.petrotrade.workflow.fuel.dto.FuelTypeResponse;

import java.util.List;

@RestController
@RequestMapping("/api/fuel-types")
@RequiredArgsConstructor
public class FuelTypeController {

    private final FuelTypeService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FuelTypeResponse create(@Valid @RequestBody FuelTypeRequest request) {
        return FuelTypeResponse.from(service.create(request));
    }

    @GetMapping
    public List<FuelTypeResponse> list() {
        return service.findAll().stream().map(FuelTypeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public FuelTypeResponse get(@PathVariable Long id) {
        return FuelTypeResponse.from(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FuelTypeResponse update(@PathVariable Long id, @Valid @RequestBody FuelTypeRequest request) {
        return FuelTypeResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
