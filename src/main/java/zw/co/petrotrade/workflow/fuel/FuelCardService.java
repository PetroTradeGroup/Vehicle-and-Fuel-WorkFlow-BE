package zw.co.petrotrade.workflow.fuel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.station.StationRepository;
import zw.co.petrotrade.workflow.user.UserRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;

@Service
@RequiredArgsConstructor
public class FuelCardService {

    private final FuelCardRepository repository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;

    public FuelCard create(FuelCard card) {
        if (repository.existsByCardNumber(card.getCardNumber())) {
            throw new IllegalStateException("Fuel card " + card.getCardNumber() + " is already registered");
        }
        if (!holderExists(card.getHolderType(), card.getHolderId())) {
            throw new IllegalArgumentException(
                    card.getHolderType() + " " + card.getHolderId() + " does not exist");
        }
        if (repository.findByHolderTypeAndHolderId(card.getHolderType(), card.getHolderId()).isPresent()) {
            throw new IllegalStateException(
                    card.getHolderType() + " " + card.getHolderId() + " already has a fuel card");
        }
        return repository.save(card);
    }

    public FuelCard findActiveCard(CardHolderType holderType, Long holderId) {
        FuelCard card = repository.findByHolderTypeAndHolderId(holderType, holderId)
                .orElseThrow(() -> new IllegalStateException(
                        holderType + " " + holderId + " has no fuel card registered"));
        if (!Boolean.TRUE.equals(card.getActive())) {
            throw new IllegalStateException("Fuel card " + card.getCardNumber() + " is not active");
        }
        return card;
    }

    public FuelCard credit(FuelCard card, double litres) {
        card.setBalanceLitres(card.getBalanceLitres() + litres);
        return repository.save(card);
    }

    private boolean holderExists(CardHolderType holderType, Long holderId) {
        return switch (holderType) {
            case VEHICLE -> vehicleRepository.existsById(holderId);
            case USER -> userRepository.existsById(holderId);
            case STATION -> stationRepository.existsById(holderId);
        };
    }
}
