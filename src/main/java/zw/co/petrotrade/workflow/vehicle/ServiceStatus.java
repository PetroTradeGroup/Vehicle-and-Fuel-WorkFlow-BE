package zw.co.petrotrade.workflow.vehicle;

// A vehicle goes for a service every 5000 km, counted from the odometer at its last service
public enum ServiceStatus {

    OK,
    DUE_SOON,
    DUE,
    NOT_RECORDED;

    public static final double INTERVAL_KM = 5000;
    public static final double DUE_SOON_WITHIN_KM = 500;

    // km left before the next service; negative once it's overdue, null while no service is on record
    public static Double kmToService(Vehicle vehicle) {
        if (vehicle.getLastServiceOdometerKm() == null || vehicle.getOdometerKm() == null) return null;
        return vehicle.getLastServiceOdometerKm() + INTERVAL_KM - vehicle.getOdometerKm();
    }

    public static ServiceStatus of(Vehicle vehicle) {
        Double left = kmToService(vehicle);
        if (left == null) return NOT_RECORDED;
        if (left <= 0) return DUE;
        return left <= DUE_SOON_WITHIN_KM ? DUE_SOON : OK;
    }
}
