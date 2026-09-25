package zw.co.petrotrade.workflow.station;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.station.dto.StationRequest;
import zw.co.petrotrade.workflow.station.dto.StationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationRepository repository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse create(@Valid @RequestBody StationRequest request) {
        if (repository.existsByName(request.name())) {
            throw new IllegalStateException("Station " + request.name() + " already exists");
        }
        return StationResponse.from(repository.save(request.toEntity()));
    }

    @GetMapping
    public List<StationResponse> list() {
        return repository.findAll().stream().map(StationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public StationResponse get(@PathVariable Long id) {
        return StationResponse.from(repository.findById(id).orElseThrow());
    }
}
