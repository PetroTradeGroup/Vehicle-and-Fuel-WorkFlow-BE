package zw.co.petrotrade.workflow.transport;

import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;

import java.util.List;

public record RequestAudit(
        TransportRequest request,
        List<Approval> approvals,
        VehicleAllocation vehicleAllocation) {
}
