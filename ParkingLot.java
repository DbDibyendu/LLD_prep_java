import java.util.*;

// ---------- ENUMS ----------

// Enum → Type safety + avoids magic strings
enum VehicleType {
    BIKE, CAR, TRUCK
}

enum SpotType {
    SMALL, MEDIUM, LARGE
}

enum TicketStatus {
    ACTIVE, PAID
}

// ---------- VEHICLES ----------

// Abstraction: hides concrete vehicle details
// LSP: Subclasses can replace Vehicle safely
abstract class Vehicle {
    protected String licensePlate;
    protected VehicleType type;

    Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    VehicleType getType() {
        return type;
    }
}

// Inheritance + Polymorphism
class Bike extends Vehicle {
    Bike(String plate) {
        super(plate, VehicleType.BIKE);
    }
}

class Car extends Vehicle {
    Car(String plate) {
        super(plate, VehicleType.CAR);
    }
}

class Truck extends Vehicle {
    Truck(String plate) {
        super(plate, VehicleType.TRUCK);
    }
}

// ---------- PARKING SPOT ----------

// SRP: ParkingSpot only manages spot state
class ParkingSpot {
    SpotType spotType;
    String spotId;
    boolean isAvailable;
    Vehicle vehicle;

    ParkingSpot(String spotId, SpotType spotType) {
        this.spotId = spotId;
        this.spotType = spotType;
        this.isAvailable = true;
    }

    // Encapsulation: internal logic hidden
    boolean canFitVehicle(Vehicle vehicle) {
        if (vehicle.getType() == VehicleType.BIKE) return true;
        if (vehicle.getType() == VehicleType.CAR)
            return spotType == SpotType.MEDIUM || spotType == SpotType.LARGE;
        return spotType == SpotType.LARGE;
    }

    void parkVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.isAvailable = false;
    }

    void unparkVehicle() {
        this.vehicle = null;
        this.isAvailable = true;
    }
}

// ---------- PARKING FLOOR ----------

// Composition: Floor owns ParkingSpots
class ParkingFloor {
    private List<ParkingSpot> spots = new ArrayList<>();
    private String floorId;

    ParkingFloor(String floorId) {
        this.floorId = floorId;
    }

    void addSpot(ParkingSpot spot) {
        spots.add(spot);
    }

    // Abstraction: hides search logic
    ParkingSpot getParkingSpot(Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable && spot.canFitVehicle(vehicle)) {
                return spot;
            }
        }
        return null;
    }
}

// ---------- TICKET ----------

// Entity object
class Ticket {
    String ticketId;
    Vehicle vehicle;
    TicketStatus status;
    ParkingSpot spot;
    long entryTime;
    long exitTime;

    Ticket(String ticketId, Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spot = spot;
        this.status = TicketStatus.ACTIVE;
        this.entryTime = System.currentTimeMillis();
    }

    void closeTicket() {
        this.exitTime = System.currentTimeMillis();
        this.status = TicketStatus.PAID;
    }
}

// ---------- PRICING ----------

// Strategy Pattern: abstraction for pricing algorithms
interface PricingCalculator {
    double calculatePrice(Ticket ticket);
}

// OCP: new pricing logic without modifying ParkingLot
// follows polymorphism
class HourlyPricingCalculator implements PricingCalculator {
    @Override
    public double calculatePrice(Ticket ticket) {
        long durationMs = ticket.exitTime - ticket.entryTime;
        long hours = Math.max(1, durationMs / (1000 * 60 * 60));
        return hours * 50.0;
    }
}

class MinutePricingCalculator implements PricingCalculator {
    @Override
    public double calculatePrice(Ticket ticket) {
        long durationMs = ticket.exitTime - ticket.entryTime;
        long minutes = Math.max(1, durationMs / (1000 * 60));
        return minutes * 2.0;
    }
}

// ---------- PARKING LOT ----------

// Facade: hides parking + pricing complexity
// DIP: depends on PricingCalculator abstraction
class ParkingLot {

    private List<ParkingFloor> floors;
    private Map<String, Ticket> activeTickets;
    private PricingCalculator pricingCalculator; // abstraction and not tight coupling

    ParkingLot(PricingCalculator pricingCalculator) {
        this.floors = new ArrayList<>();
        this.activeTickets = new HashMap<>();
        this.pricingCalculator = pricingCalculator;
    }

    void addFloor(ParkingFloor floor) {
        floors.add(floor);
    }

    // synchronized → thread safety
    synchronized Ticket parkVehicle(Vehicle vehicle) {
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.getParkingSpot(vehicle);
            if (spot != null) {
                spot.parkVehicle(vehicle);
                Ticket ticket = new Ticket(UUID.randomUUID().toString(), vehicle, spot);
                activeTickets.put(vehicle.licensePlate, ticket);
                return ticket;
            }
        }
        throw new RuntimeException("No parking spot available");
    }

    synchronized double unparkVehicle(Vehicle vehicle) {
        Ticket ticket = activeTickets.get(vehicle.licensePlate);
        if (ticket == null) throw new RuntimeException("Invalid ticket");

        ticket.closeTicket();
        ticket.spot.unparkVehicle();
        activeTickets.remove(vehicle.licensePlate);

        return pricingCalculator.calculatePrice(ticket);
    }
}

// ---------- BUILDER ----------

// Builder Pattern: step-by-step construction
class ParkingLotBuilder {
    private PricingCalculator pricingCalculator;
    private List<ParkingFloor> floors = new ArrayList<>();

    static ParkingLotBuilder withPricingCalculator(PricingCalculator calculator) {
        ParkingLotBuilder builder = new ParkingLotBuilder();
        builder.pricingCalculator = calculator;
        return builder;
    }

    ParkingLotBuilder addFloor(ParkingFloor floor) {
        floors.add(floor);
        return this;
    }

    ParkingLot build() {
        ParkingLot lot = new ParkingLot(pricingCalculator);
        for (ParkingFloor floor : floors) {
            lot.addFloor(floor);
        }
        return lot;
    }
}

// ---------- MAIN ----------

public class Main {
    public static void main(String[] args) {

        // Strategy Pattern in action
        PricingCalculator pricing = new HourlyPricingCalculator();

        // Builder Pattern
        ParkingFloor floor1 = new ParkingFloor("F1");
        floor1.addSpot(new ParkingSpot("S1", SpotType.SMALL));
        floor1.addSpot(new ParkingSpot("S2", SpotType.MEDIUM));

        ParkingLot parkingLot = ParkingLotBuilder
                .withPricingCalculator(pricing)
                .addFloor(floor1)
                .build();

        Vehicle car = new Car("ABC123");
        Ticket ticket = parkingLot.parkVehicle(car);

        double amount = parkingLot.unparkVehicle(car);
        System.out.println("Pay " + amount + " to exit");
    }
}
