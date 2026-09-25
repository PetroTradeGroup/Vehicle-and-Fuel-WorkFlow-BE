package zw.co.petrotrade.workflow.station.dto;

import jakarta.validation.constraints.NotBlank;

public record TopUpDecisionRequest(
        boolean approved,
        @NotBlank String approver,
        String comments) {
}
