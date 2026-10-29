package zw.co.petrotrade.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import zw.co.petrotrade.workflow.vehicle.VehicleServiceImp;
import zw.co.petrotrade.workflow.vehicle.VehicleStatus;

@SpringBootTest
class VehicleAndFuelWorkFlowBeApplicationTests {

    @Autowired
    VehicleServiceImp vehicles;

    @Test
    void contextLoads() {
    }

    // runs the real query on Postgres: an empty search or no status must not break it
    @Test
    void vehicleSearchRunsOnTheDatabase() {
        vehicles.search(null, VehicleStatus.AVAILABLE);
        vehicles.search("toy", null);
        vehicles.search("toy", VehicleStatus.AVAILABLE);
    }

}
