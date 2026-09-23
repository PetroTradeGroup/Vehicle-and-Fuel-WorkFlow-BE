package zw.co.petrotrade.workflow.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransportRequestService {

    private final TransportRequestRepository repository;

    public TransportRequest create(TransportRequest request) {

        request.setStatus(RequestStatus.PENDING_HOD);

        if (request.getDistanceKm() != null) {
            request.setFuelRequiredLitres(
                    request.getDistanceKm() / 10
            );
        }

        return repository.save(request);
    }
}