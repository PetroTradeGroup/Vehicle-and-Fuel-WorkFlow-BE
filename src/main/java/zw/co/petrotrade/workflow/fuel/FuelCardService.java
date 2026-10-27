package zw.co.petrotrade.workflow.fuel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.fuel.dto.FuelCardRequest;
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

    // balance is left alone: it only moves through top-ups and transactions
    public FuelCard update(Long id, FuelCardRequest request) {
        FuelCard card = repository.findById(id).orElseThrow();
        if (!card.getCardNumber().equals(request.cardNumber()) && repository.existsByCardNumber(request.cardNumber())) {
            throw new IllegalStateException("Fuel card " + request.cardNumber() + " is already registered");
        }
        if (!holderExists(request.holderType(), request.holderId())) {
            throw new IllegalArgumentException(
                    request.holderType() + " " + request.holderId() + " does not exist");
        }
        repository.findByHolderTypeAndHolderId(request.holderType(), request.holderId())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException(
                            request.holderType() + " " + request.holderId() + " already has a fuel card");
                });
        card.setCardNumber(request.cardNumber());
        card.setHolderType(request.holderType());
        card.setHolderId(request.holderId());
        if (request.active() != null) {
            card.setActive(request.active());
        }
        return repository.save(card);
    }

    public FuelCard findActiveCard(CardHolderType holderType, Long holderId) {
        FuelCard card = repository.findByHolderTypeAndHolderId(holderType, holderId)
                .orElseThrow(() -> new IllegalStateException("This "
                        + switch (holderType) { case VEHICLE -> "vehicle"; case USER -> "driver"; case STATION -> "station"; }
                        + " has no fuel card yet. Register one on the Fuel Cards page first."));
        if (!Boolean.TRUE.equals(card.getActive())) {
            throw new IllegalStateException("Fuel card " + card.getCardNumber() + " is switched off, so fuel can't be loaded onto it.");
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
