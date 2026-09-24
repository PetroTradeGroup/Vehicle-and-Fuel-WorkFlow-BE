package zw.co.petrotrade.workflow.transport.dto;

import zw.co.petrotrade.workflow.approval.dto.ApprovalResponse;
import zw.co.petrotrade.workflow.transport.RequestAudit;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleAllocationResponse;

import java.util.List;

public record RequestAuditResponse(
        TransportRequestResponse request,
        List<ApprovalResponse> approvals,
        VehicleAllocationResponse vehicleAllocation) {

    public static RequestAuditResponse from(RequestAudit audit) {
        return new RequestAuditResponse(
                TransportRequestResponse.from(audit.request()),
                audit.approvals().stream().map(ApprovalResponse::from).toList(),
                audit.vehicleAllocation() == null ? null : VehicleAllocationResponse.from(audit.vehicleAllocation()));
    }
}
