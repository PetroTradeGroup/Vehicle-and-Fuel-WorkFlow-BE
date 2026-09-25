package zw.co.petrotrade.workflow.fuel.dto;

import zw.co.petrotrade.workflow.fuel.CardHolderType;
import zw.co.petrotrade.workflow.fuel.FuelCard;

public record FuelCardResponse(
        Long id,
        String cardNumber,
        CardHolderType holderType,
        Long holderId,
        Double balanceLitres,
        Boolean active) {

    public static FuelCardResponse from(FuelCard card) {
        return new FuelCardResponse(
                card.getId(),
                card.getCardNumber(),
                card.getHolderType(),
                card.getHolderId(),
                card.getBalanceLitres(),
                card.getActive());
    }
}
