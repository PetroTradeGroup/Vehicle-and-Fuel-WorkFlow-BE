package zw.co.petrotrade.workflow.fuel.dto;

import zw.co.petrotrade.workflow.fuel.FuelCard;

public record FuelCardResponse(
        Long id,
        String cardNumber,
        Double balanceLitres,
        Boolean active) {

    public static FuelCardResponse from(FuelCard card) {
        return new FuelCardResponse(
                card.getId(),
                card.getCardNumber(),
                card.getBalanceLitres(),
                card.getActive());
    }
}
