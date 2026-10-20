package zw.co.petrotrade.workflow.user;

import java.util.List;

// Order matters: the last role someone holds is the one shown as theirs
public enum Role {

    // runs one station: sees only that station's top-ups, card and report
    STATION_ADMIN,
    DRIVER,
    HOD,
    // approve station top-ups first, before HR & Admin
    REGIONAL_REP,
    // also head of their own department: gives the department head approval on its requests
    RETAIL_MANAGER,
    HR_ADMIN_MANAGER,
    VEHICLE_ADMIN,
    SYSTEM_ADMIN;

    // roles that act as head of their department; a department has one of them
    public static final List<Role> DEPARTMENT_HEADS = List.of(HOD, RETAIL_MANAGER);

    public boolean headsDepartment() {
        return DEPARTMENT_HEADS.contains(this);
    }
}
