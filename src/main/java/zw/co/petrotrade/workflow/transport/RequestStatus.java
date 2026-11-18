package zw.co.petrotrade.workflow.transport;

public enum RequestStatus {

    PENDING_HOD("waiting for the department head", "Pending HOD · Awaiting department head"),
    APPROVED_HOD("waiting for a vehicle to be allocated", "HOD approved · Awaiting vehicle allocation"),
    REJECTED_HOD("rejected by the department head", "Rejected by HOD"),
    // legacy: HR & Admin used to approve before allocation; these requests carry on at allocation
    APPROVED_HR_ADMIN("waiting for a vehicle to be allocated", "HR & Admin approved · Awaiting vehicle allocation"),
    REJECTED_HR_ADMIN("rejected by HR & Admin", "Rejected by HR & Admin"),
    VEHICLE_ALLOCATED("waiting for fuel to be calculated", "Vehicle allocated · Awaiting fuel calculation"),
    // HR & Admin approve the trip and its fuel together, as the last step before it goes out
    FUEL_CALCULATED("waiting for HR & Admin approval", "Awaiting HR & Admin · Awaiting HR & Admin approval"),
    FUEL_APPROVED("out on the trip", "On trip"),
    // legacy: fuel rejections from before the single HR & Admin approval
    FUEL_REJECTED("closed because fuel was rejected", "Fuel rejected"),
    COMPLETED("completed", "Completed");

    // plain-English "where it is now", for error messages
    private final String description;
    // the frontend's badge and queue wording (REQUEST_STATUS / QUEUES in utils.js), so search finds what people see
    private final String screenLabels;

    RequestStatus(String description, String screenLabels) {
        this.description = description;
        this.screenLabels = screenLabels;
    }

    public String description() {
        return description;
    }

    public String screenLabels() {
        return screenLabels;
    }

    // e.g. "This request is now waiting for HR & Admin. Refresh to see the latest."
    public String alreadyMovedOn() {
        return "This request is now " + description + ". Refresh to see the latest.";
    }
}
