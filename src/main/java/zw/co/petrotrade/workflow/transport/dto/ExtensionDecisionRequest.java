package zw.co.petrotrade.workflow.transport.dto;

import jakarta.validation.constraints.NotNull;

public record ExtensionDecisionRequest(@NotNull Boolean approved, String comments) {
}
