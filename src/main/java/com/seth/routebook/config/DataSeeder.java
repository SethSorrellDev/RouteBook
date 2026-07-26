package com.seth.routebook.config;

import com.seth.routebook.domain.*;
import com.seth.routebook.domain.enums.KnowledgeCategory;
import com.seth.routebook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the in-memory H2 database with representative data on every
 * startup (matches ddl-auto=create-drop). Uses the same real-world
 * Frankfort/Crawfordsville stop names as RouteOptimizer so the suite's
 * example data stays consistent across apps.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DriverRepository driverRepository;
    private final LocationRepository locationRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final KnowledgeEntryRepository knowledgeEntryRepository;

    @Override
    public void run(String... args) {
        // Placeholder driver - fictional name for portfolio/demo purposes
        Driver driver = new Driver();
        driver.setEmployeeId("EMP-1001");
        driver.setFirstName("S.");
        driver.setLastName("Anderson");
        driver.setEmail("s.anderson@example.com");
        driverRepository.save(driver);

        // Route 1: Frankfort area
        Route route1 = new Route();
        route1.setName("Route 14 - Frankfort");
        route1.setDescription("Normal route covering Frankfort industrial corridor");
        route1.setDriver(driver);
        routeRepository.save(route1);

        Location nucorLocation = buildLocation(
                "1300 Somerset Rd", null, "Crawfordsville", "IN", "47933", 40.0411, -86.8745);
        locationRepository.save(nucorLocation);

        Location paceDairyLocation = buildLocation(
                "1200 W Wabash Ave", null, "Crawfordsville", "IN", "47933", 40.0417, -86.9086);
        locationRepository.save(paceDairyLocation);

        Location chipotleLocation = buildLocation(
                "1911 E Lincoln Rd", null, "Kokomo", "IN", "46902", 40.4864, -86.1002);
        locationRepository.save(chipotleLocation);

        Stop stop1 = buildStop("Nucor Steel", 1, route1, nucorLocation);
        stopRepository.save(stop1);

        Stop stop2 = buildStop("Pace Dairy", 2, route1, paceDairyLocation);
        stopRepository.save(stop2);

        Stop stop3 = buildStop("Chipotle Kokomo", 3, route1, chipotleLocation);
        stopRepository.save(stop3);

        // Knowledge entries - mix of route-level and stop-level
        KnowledgeEntry routeNote = new KnowledgeEntry();
        routeNote.setTitle("Route requires steel-toe boots");
        routeNote.setBody("Three accounts on this route (Nucor, Pace Dairy, and one other) " +
                "require steel-toe boots and a hard hat for dock access. Keep both in the van.");
        routeNote.setCategory(KnowledgeCategory.HAZARD);
        routeNote.setRoute(route1);
        knowledgeEntryRepository.save(routeNote);

        KnowledgeEntry gateCode = new KnowledgeEntry();
        gateCode.setTitle("Nucor Steel gate code");
        gateCode.setBody("Main gate keypad code is 4471#. Code resets monthly - " +
                "confirm with dock supervisor if it doesn't work.");
        gateCode.setCategory(KnowledgeCategory.GATE_CODE);
        gateCode.setStop(stop1);
        knowledgeEntryRepository.save(gateCode);

        KnowledgeEntry parkingNote = new KnowledgeEntry();
        parkingNote.setTitle("Pace Dairy parking");
        parkingNote.setBody("Do not park in front of the dock doors - loading trucks need " +
                "that space. Park along the fence line to the east instead.");
        parkingNote.setCategory(KnowledgeCategory.PARKING);
        parkingNote.setStop(stop2);
        knowledgeEntryRepository.save(parkingNote);

        System.out.println("=== DataSeeder: seeded 1 driver, 1 route, 3 stops, 3 knowledge entries ===");
    }

    private Location buildLocation(String addr1, String addr2, String city, String state,
                                    String zip, double lat, double lon) {
        Location location = new Location();
        location.setAddressLine1(addr1);
        location.setAddressLine2(addr2);
        location.setCity(city);
        location.setState(state);
        location.setZipCode(zip);
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }

    private Stop buildStop(String customerName, int sequence, Route route, Location location) {
        Stop stop = new Stop();
        stop.setCustomerName(customerName);
        stop.setSequenceOrder(sequence);
        stop.setRoute(route);
        stop.setLocation(location);
        return stop;
    }
}
