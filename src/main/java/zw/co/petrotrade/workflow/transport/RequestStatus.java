package zw.co.petrotrade.workflow.transport;

public enum RequestStatus {

    PENDING_HOD("waiting for the department head"),
    APPROVED_HOD("waiting for HR & Admin"),
    REJECTED_HOD("rejected by the department head"),
    APPROVED_HR_ADMIN("waiting for a vehicle to be allocated"),
    REJECTED_HR_ADMIN("rejected by HR & Admin"),
    VEHICLE_ALLOCATED("waiting for fuel to be calculated"),
    FUEL_CALCULATED("waiting for fuel approval"),
    FUEL_APPROVED("out on the trip"),
    FUEL_REJECTED("closed because fuel was rejected"),
    COMPLETED("completed");

    // plain-English "where it is now", for error messages
    private final String description;

    RequestStatus(String description) {
        this.description = description;
    }

    // e.g. "This request is now waiting for HR & Admin. Refresh to see the latest."
    public String alreadyMovedOn() {
        return "This request is now " + description + ". Refresh to see the latest.";
    }
}
