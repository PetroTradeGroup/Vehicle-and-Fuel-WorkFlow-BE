package zw.co.petrotrade.workflow.transport;

import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.fuel.FuelTransaction;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;

import java.util.List;

public record RequestAudit(
        TransportRequest request,
        List<Approval> approvals,
        VehicleAllocation vehicleAllocation,
        // the trip's fuel, once HR & Admin approved it; null when nothing was loaded
        FuelTransaction fuel) {

    // HR & Admin approved the fuel but IT hasn't loaded it onto the card yet
    public boolean awaitingFuel() {
        return fuel != null && Boolean.FALSE.equals(fuel.getLoaded()) && request.getStatus() == RequestStatus.FUEL_APPROVED;
    }
}
