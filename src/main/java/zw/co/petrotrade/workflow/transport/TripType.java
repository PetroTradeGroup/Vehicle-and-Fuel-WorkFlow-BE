package zw.co.petrotrade.workflow.transport;

// 50 km or more from the work station is an external trip (will need a T&S form signed by the CEO)
public enum TripType {

    LOCAL,
    EXTERNAL;

    public static final double EXTERNAL_FROM_KM = 50;

    public static TripType of(Double distanceKm) {
        return distanceKm != null && distanceKm >= EXTERNAL_FROM_KM ? EXTERNAL : LOCAL;
    }
}
