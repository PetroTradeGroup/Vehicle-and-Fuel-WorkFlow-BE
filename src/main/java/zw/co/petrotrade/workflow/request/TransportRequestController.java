package zw.co.petrotrade.workflow.request;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class TransportRequestController {

    private final TransportRequestService service;

    @PostMapping
    public TransportRequest create(
            @RequestBody TransportRequest request) {

        return service.create(request);
    }
}