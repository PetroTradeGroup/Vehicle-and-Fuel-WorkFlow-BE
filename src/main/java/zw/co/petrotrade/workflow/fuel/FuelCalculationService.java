package zw.co.petrotrade.workflow.fuel;

import org.springframework.stereotype.Service;

@Service
public class FuelCalculationService {

    public Double calculateFuel(Double distanceKm) {
        return distanceKm / 10;
    }
}