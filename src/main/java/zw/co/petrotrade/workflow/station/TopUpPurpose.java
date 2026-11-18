package zw.co.petrotrade.workflow.station;

// what a station top-up is for; OTHER is explained in the reason
public enum TopUpPurpose {

    GENERATOR("Generator"),
    GRASS_CUTTER("Grass cutter"),
    OTHER("Other");

    // the most a grass cutter request may ask for
    public static final double GRASS_CUTTER_MAX_LITRES = 10;

    private final String label;

    TopUpPurpose(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
