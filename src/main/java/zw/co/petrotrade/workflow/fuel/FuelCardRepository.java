package zw.co.petrotrade.workflow.fuel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuelCardRepository extends JpaRepository<FuelCard, Long> {

    Optional<FuelCard> findByHolderTypeAndHolderId(CardHolderType holderType, Long holderId);

    boolean existsByCardNumber(String cardNumber);
}
