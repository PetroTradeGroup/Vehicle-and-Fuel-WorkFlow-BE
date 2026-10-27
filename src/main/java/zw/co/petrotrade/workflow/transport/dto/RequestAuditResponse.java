package zw.co.petrotrade.workflow.transport.dto;

import zw.co.petrotrade.workflow.approval.dto.ApprovalResponse;
import zw.co.petrotrade.workflow.transport.RequestAudit;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleAllocationResponse;

import java.time.LocalDateTime;
import java.util.List;

public record RequestAuditResponse(
        TransportRequestResponse request,
        List<ApprovalResponse> approvals,
        VehicleAllocationResponse vehicleAllocation,
        boolean awaitingFuel,
        // who put the fuel on the card and when; null while waiting, and for fuel from before IT loaded it
        String fuelLoadedBy,
        LocalDateTime fuelLoadedAt) {

    public static RequestAuditResponse from(RequestAudit audit) {
        return new RequestAuditResponse(
                TransportRequestResponse.from(audit.request()),
                audit.approvals().stream().map(ApprovalResponse::from).toList(),
                audit.vehicleAllocation() == null ? null : VehicleAllocationResponse.from(audit.vehicleAllocation()),
                audit.awaitingFuel(),
                audit.fuel() == null ? null : audit.fuel().getLoadedBy(),
                audit.fuel() == null ? null : audit.fuel().getLoadedAt());
    }
}
