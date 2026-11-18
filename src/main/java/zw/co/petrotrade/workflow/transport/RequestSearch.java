package zw.co.petrotrade.workflow.transport;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Free-text request search; every word must match:
//   "#12"            request 12
//   ">100", "<=50km" distance comparisons
//   anything else    found in the id, driver, department, route, distance, status ("fuel approved", "on trip",
//                    "awaiting vehicle allocation") or trip type ("external")
public final class RequestSearch {

    private static final Pattern ID = Pattern.compile("#(\\d+)");
    private static final Pattern KM = Pattern.compile("(<=|>=|<|>)(\\d+(?:\\.\\d+)?)(?:km)?");

    private RequestSearch() {
    }

    public static boolean matches(TransportRequest r, String query) {
        if (query == null || query.isBlank()) return true;
        String text = Stream.of(r.getId(), r.getDriverName(), r.getDepartment(), r.getStartingPoint(), r.getDestination(),
                        r.getDistanceKm() == null ? null : number(r.getDistanceKm()) + " km",
                        r.getStatus() == null ? null : r.getStatus().name().replace('_', ' ') + " · "
                                + r.getStatus().description() + " · " + r.getStatus().screenLabels(),
                        TripType.of(r.getDistanceKm()).name())
                .filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(" ")).toLowerCase(Locale.ROOT);
        return Arrays.stream(query.toLowerCase(Locale.ROOT).trim().split("\\s+")).allMatch(word -> {
            Matcher id = ID.matcher(word);
            if (id.matches()) return String.valueOf(r.getId()).equals(id.group(1));
            Matcher km = KM.matcher(word);
            if (km.matches()) return r.getDistanceKm() != null && compare(r.getDistanceKm(), km.group(1), Double.parseDouble(km.group(2)));
            return text.contains(word);
        });
    }

    private static boolean compare(double distance, String op, double km) {
        return switch (op) {
            case "<" -> distance < km;
            case "<=" -> distance <= km;
            case ">" -> distance > km;
            default -> distance >= km;
        };
    }

    // 120.0 reads as "120", so searching "120 km" finds it
    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
