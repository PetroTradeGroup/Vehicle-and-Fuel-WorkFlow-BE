package zw.co.petrotrade.workflow.approval;

public enum ApprovalLevel {

    HOD,
    HR_ADMIN,
    // a regional rep or the retail manager: the first approval on a station top-up
    REGIONAL_RETAIL,
    FUEL,
    // a vehicle admin's decision on a driver's request to keep the vehicle longer
    EXTENSION
}
