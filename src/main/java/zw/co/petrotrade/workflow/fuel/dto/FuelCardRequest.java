package zw.co.petrotrade.workflow.fuel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.fuel.CardHolderType;
import zw.co.petrotrade.workflow.fuel.FuelCard;

public record FuelCardRequest(
        @NotBlank String cardNumber,
        @NotNull CardHolderType holderType,
        @NotNull Long holderId) {

    public FuelCard toEntity() {
        FuelCard card = new FuelCard();
        card.setCardNumber(cardNumber);
        card.setHolderType(holderType);
        card.setHolderId(holderId);
        card.setBalanceLitres(0.0);
        card.setActive(true);
        return card;
    }
}
