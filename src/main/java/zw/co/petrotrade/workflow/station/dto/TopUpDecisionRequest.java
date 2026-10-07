package zw.co.petrotrade.workflow.station.dto;

// the approver is the signed-in user
public record TopUpDecisionRequest(
        boolean approved,
        String comments) {
}
