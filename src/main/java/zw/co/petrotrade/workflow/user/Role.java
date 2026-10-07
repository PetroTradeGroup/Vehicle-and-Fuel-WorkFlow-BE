package zw.co.petrotrade.workflow.user;

// Order matters: the last role someone holds is the one shown as theirs
public enum Role {

    // runs one station: sees only that station's top-ups, card and report
    STATION_ADMIN,
    DRIVER,
    HOD,
    HR_ADMIN_MANAGER,
    VEHICLE_ADMIN,
    SYSTEM_ADMIN
}
