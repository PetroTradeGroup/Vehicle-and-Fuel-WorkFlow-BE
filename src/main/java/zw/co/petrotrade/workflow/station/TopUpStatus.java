package zw.co.petrotrade.workflow.station;

// a regional rep or the retail manager approves first, then HR & Admin, whose approval credits the card
public enum TopUpStatus {

    PENDING_FIRST_APPROVAL,
    // awaiting HR & Admin
    PENDING_APPROVAL,
    COMPLETED,
    REJECTED;

    public boolean pending() {
        return this == PENDING_FIRST_APPROVAL || this == PENDING_APPROVAL;
    }
}
