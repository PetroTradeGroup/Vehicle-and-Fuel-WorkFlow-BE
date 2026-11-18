package zw.co.petrotrade.workflow.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestSearchTest {

    @Test
    void searchesByIdStatusDriverTripTypeDistanceAndRoute() {
        TransportRequest trip = new TransportRequest();
        trip.setId(12L);
        trip.setDriverName("Jane Moyo");
        trip.setStartingPoint("Harare");
        trip.setDestination("Mutare");
        trip.setDistanceKm(263.0);
        trip.setStatus(RequestStatus.FUEL_APPROVED);

        assertTrue(RequestSearch.matches(trip, null));
        assertTrue(RequestSearch.matches(trip, "#12"));
        assertFalse(RequestSearch.matches(trip, "#1"));
        assertTrue(RequestSearch.matches(trip, "fuel approved"));
        assertTrue(RequestSearch.matches(trip, "on trip"));
        assertFalse(RequestSearch.matches(trip, "awaiting"));
        trip.setStatus(RequestStatus.APPROVED_HOD);
        assertTrue(RequestSearch.matches(trip, "awaiting vehicle allocation"));
        assertTrue(RequestSearch.matches(trip, "HOD approved"));
        trip.setStatus(RequestStatus.FUEL_APPROVED);
        assertTrue(RequestSearch.matches(trip, "jane"));
        assertTrue(RequestSearch.matches(trip, "External"));
        assertFalse(RequestSearch.matches(trip, "local"));
        assertTrue(RequestSearch.matches(trip, ">100"));
        assertTrue(RequestSearch.matches(trip, "<=263km"));
        assertFalse(RequestSearch.matches(trip, "<50"));
        assertTrue(RequestSearch.matches(trip, "263 km"));
        assertTrue(RequestSearch.matches(trip, "harare mutare"));
        assertFalse(RequestSearch.matches(trip, "harare bulawayo"));
    }
}
