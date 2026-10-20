package zw.co.petrotrade.workflow.transport;

public enum RequestStatus {

    PENDING_HOD("waiting for the department head"),
    APPROVED_HOD("waiting for a vehicle to be allocated"),
    REJECTED_HOD("rejected by the department head"),
    // legacy: HR & Admin used to approve before allocation; these requests carry on at allocation
    APPROVED_HR_ADMIN("waiting for a vehicle to be allocated"),
    REJECTED_HR_ADMIN("rejected by HR & Admin"),
    VEHICLE_ALLOCATED("waiting for fuel to be calculated"),
    // HR & Admin approve the trip and its fuel together, as the last step before it goes out
    FUEL_CALCULATED("waiting for HR & Admin approval"),
    FUEL_APPROVED("out on the trip"),
    // legacy: fuel rejections from before the single HR & Admin approval
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
