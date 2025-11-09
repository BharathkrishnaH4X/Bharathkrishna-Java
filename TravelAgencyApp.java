
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Main Application
public class TravelAgencyApp {
    private static Scanner sc = new Scanner(System.in);
    private static Map<Integer, TourPackage> packages = new HashMap<>();
    private static Map<Integer, Customer> customers = new HashMap<>();
    private static Map<Integer, Booking> bookings = new HashMap<>();
    private static Map<Integer, Payment> payments = new HashMap<>();
    private static Map<Integer, Cancellation> cancellations = new HashMap<>();

    private static int pkgIdSeq = 1, custIdSeq = 1, bookIdSeq = 1, payIdSeq = 1, itinIdSeq = 1, cancelIdSeq = 1;

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            showMenu();
            int choice = readInt("Choose an option: ");
            switch (choice) {
                case 1 -> addTourPackage();
                case 2 -> addItineraryItem();
                case 3 -> addCustomer();
                case 4 -> createBooking();
                case 5 -> recordPayment();
                case 6 -> cancelBooking();
                case 7 -> displayPackages();
                case 8 -> { System.out.println("Exiting... Goodbye"); running = false; }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
        sc.close();
    }

    private static void showMenu() {
        System.out.println("\n=== Travel Agency Menu ===");
        System.out.println("1. Add Tour Package");
        System.out.println("2. Add Itinerary Item");
        System.out.println("3. Add Customer");
        System.out.println("4. Create Booking");
        System.out.println("5. Record Payment");
        System.out.println("6. Cancel Booking");
        System.out.println("7. Display Packages & Availability");
        System.out.println("8. Exit");
    }

    // --- Menu actions ---
    private static void addTourPackage() {
        System.out.println("\n-- Add Tour Package --");
        String name = readNonEmpty("Name: ");
        String desc = readNonEmpty("Description: ");
        LocalDate start = readDate("Start date (yyyy-MM-dd): ");
        LocalDate end = readDate("End date (yyyy-MM-dd): ");
        while (end.isBefore(start)) {
            System.out.println("End date must be after start date.");
            end = readDate("End date (yyyy-MM-dd): ");
        }
        double price = readDouble("Price per person: ");
        int seats = readInt("Total seats: ");
        TourPackage tp = new TourPackage(pkgIdSeq++, name, desc, start, end, price, seats);
        packages.put(tp.getId(), tp);
        System.out.println("Package added with ID: " + tp.getId());
    }

    private static void addItineraryItem() {
        System.out.println("\n-- Add Itinerary Item --");
        int pid = readInt("Package ID: ");
        TourPackage tp = packages.get(pid);
        if (tp == null) { System.out.println("Package not found."); return; }
        int day = readInt("Day number for this item: ");
        String title = readNonEmpty("Title: ");
        String details = readNonEmpty("Details: ");
        ItineraryItem it = new ItineraryItem(itinIdSeq++, day, title, details, pid);
        tp.addItinerary(it);
        System.out.println("Itinerary item added to package " + pid);
    }

    private static void addCustomer() {
        System.out.println("\n-- Add Customer --");
        String name = readNonEmpty("Name: ");
        String email = readNonEmpty("Email: ");
        String phone = readNonEmpty("Phone: ");
        Customer c = new Customer(custIdSeq++, name, email, phone);
        customers.put(c.getId(), c);
        System.out.println("Customer added with ID: " + c.getId());
    }

    private static void createBooking() {
        System.out.println("\n-- Create Booking --");
        int custId = readInt("Customer ID: ");
        Customer c = customers.get(custId);
        if (c == null) { System.out.println("Customer not found."); return; }
        int pkgId = readInt("Package ID: ");
        TourPackage tp = packages.get(pkgId);
        if (tp == null) { System.out.println("Package not found."); return; }
        int seats = readInt("Number of seats to book: ");
        if (seats <= 0) { System.out.println("Seats must be positive."); return; }
        if (tp.getAvailableSeats() < seats) {
            System.out.println("Not enough seats available. Available: " + tp.getAvailableSeats());
            return;
        }
        List<Traveler> travelers = new ArrayList<>();
        for (int i=1;i<=seats;i++){
            System.out.println("Enter traveler " + i + " details:");
            String tname = readNonEmpty("  Name: ");
            int age = readInt("  Age: ");
            String passport = readOptional("  Passport (or press enter): ");
            travelers.add(new Traveler(tname, age, passport));
        }
        double amountDue = seats * tp.getPrice();
        Booking b = new Booking(bookIdSeq++, pkgId, custId, travelers, seats, amountDue, LocalDate.now());
        bookings.put(b.getId(), b);
        System.out.println("Booking created with ID: " + b.getId() + ". Status: PENDING. Amount due: " + amountDue);
    }

    private static void recordPayment() {
        System.out.println("\n-- Record Payment --");
        int bid = readInt("Booking ID: ");
        Booking b = bookings.get(bid);
        if (b == null) { System.out.println("Booking not found."); return; }
        double amount = readDouble("Amount to pay: ");
        if (amount <= 0) { System.out.println("Amount must be positive."); return; }
        String method = readNonEmpty("Payment method (Cash/Card/UPI): ");
        Payment p = new Payment(payIdSeq++, bid, amount, LocalDate.now(), method);
        payments.put(p.getId(), p);
        b.addPayment(p);
        System.out.println("Payment recorded. Receipt -> Payment ID: " + p.getId() + ", Amount: " + amount);
        // finalize booking if fully paid and seats available
        TourPackage tp = packages.get(b.getPackageId());
        if (b.getPaidAmount() >= b.getAmountDue()) {
            if (tp.getAvailableSeats() >= b.getNumSeats()) {
                tp.decreaseSeats(b.getNumSeats());
                b.setStatus(BookingStatus.CONFIRMED);
                System.out.println("Booking " + b.getId() + " is CONFIRMED. Seats reserved: " + b.getNumSeats());
            } else {
                System.out.println("Payment complete but seats are no longer available. Contact support.");
            }
        } else {
            System.out.println("Booking still PENDING. Remaining due: " + (b.getAmountDue()-b.getPaidAmount()));
        }
    }

    private static void cancelBooking() {
        System.out.println("\n-- Cancel Booking --");
        int bid = readInt("Booking ID: ");
        Booking b = bookings.get(bid);
        if (b == null) { System.out.println("Booking not found."); return; }
        if (b.getStatus() == BookingStatus.CANCELLED) { System.out.println("Booking already cancelled."); return; }
        TourPackage tp = packages.get(b.getPackageId());
        LocalDate now = LocalDate.now();
        long daysBefore = ChronoUnit.DAYS.between(now, tp.getStartDate());
        double feePercent = cancellationFeePercent(daysBefore);
        double fee = b.getPaidAmount() * feePercent / 100.0;
        double refund = b.getPaidAmount() - fee;
        Cancellation c = new Cancellation(cancelIdSeq++, bid, now, fee, refund);
        cancellations.put(c.getId(), c);
        // restore seats if booking was confirmed
        if (b.getStatus() == BookingStatus.CONFIRMED) {
            tp.increaseSeats(b.getNumSeats());
        }
        b.setStatus(BookingStatus.CANCELLED);
        System.out.println("Cancellation complete. Fee: " + fee + ", Refund: " + refund);
    }

    private static void displayPackages() {
        System.out.println("\n-- Packages & Availability --");
        if (packages.isEmpty()) { System.out.println("No packages."); return; }
        for (TourPackage tp : packages.values()) {
            System.out.println(tp);
            if (!tp.getItinerary().isEmpty()) {
                System.out.println("  Itinerary:");
                for (ItineraryItem it : tp.getItinerary()) System.out.println("    " + it);
            }
        }
    }

    // --- Utilities & helpers ---
    private static int readInt(String prompt) {
        while (true) {
            try { System.out.print(prompt); String line = sc.nextLine().trim(); return Integer.parseInt(line); }
            catch (Exception e) { System.out.println("Invalid integer. Try again."); }
        }
    }
    private static double readDouble(String prompt) {
        while (true) {
            try { System.out.print(prompt); String line = sc.nextLine().trim(); return Double.parseDouble(line); }
            catch (Exception e) { System.out.println("Invalid number. Try again."); }
        }
    }
    private static String readNonEmpty(String prompt) {
        while (true) { System.out.print(prompt); String s = sc.nextLine().trim(); if (!s.isEmpty()) return s; System.out.println("Input cannot be empty."); }
    }
    private static String readOptional(String prompt) {
        System.out.print(prompt); return sc.nextLine().trim();
    }
    private static LocalDate readDate(String prompt) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            try { System.out.print(prompt); String s = sc.nextLine().trim(); return LocalDate.parse(s, f); }
            catch (Exception e) { System.out.println("Invalid date. Use yyyy-MM-dd."); }
        }
    }
    private static double cancellationFeePercent(long daysBefore) {
        if (daysBefore >= 30) return 10;
        if (daysBefore >= 15) return 25;
        if (daysBefore >= 7) return 50;
        return 75;
    }

    // --- Domain classes ---
    static class TourPackage {
        private int id; private String name; private String description;
        private LocalDate startDate; private LocalDate endDate;
        private double price; private int totalSeats; private int availableSeats;
        private List<ItineraryItem> itinerary = new ArrayList<>();
        public TourPackage(int id, String name, String desc, LocalDate start, LocalDate end, double price, int seats) {
            this.id=id; this.name=name; this.description=desc; this.startDate=start; this.endDate=end; this.price=price; this.totalSeats=seats; this.availableSeats=seats;
        }
        // encapsulated getters/setters
        public int getId(){return id;} public String getName(){return name;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public double getPrice(){return price;} public int getAvailableSeats(){return availableSeats;} public List<ItineraryItem> getItinerary(){return Collections.unmodifiableList(itinerary);} public void addItinerary(ItineraryItem it){itinerary.add(it);} public void decreaseSeats(int n){availableSeats -= n;} public void increaseSeats(int n){availableSeats += n;} @Override public String toString(){return String.format("ID:%d | %s | %s to %s | Price: %.2f | Seats: %d/%d", id, name, startDate, endDate, price, availableSeats, totalSeats);}    }

    static class ItineraryItem {
        private int id; private int day; private String title; private String details; private int packageId;
        public ItineraryItem(int id, int day, String title, String details, int packageId){this.id=id;this.day=day;this.title=title;this.details=details;this.packageId=packageId;}
        public String toString(){return String.format("Day %d: %s - %s", day, title, details);}    }

    static class Customer {
        private int id; private String name; private String email; private String phone;
        public Customer(int id, String name, String email, String phone){this.id=id;this.name=name;this.email=email;this.phone=phone;} public int getId(){return id;} public String toString(){return id+": "+name+" ("+email+","+phone+")";}    }

    static class Traveler {
        private String name; private int age; private String passport;
        public Traveler(String name,int age,String passport){this.name=name;this.age=age;this.passport=passport;} public String toString(){return name+" ("+age+")";}    }

    enum BookingStatus {PENDING, CONFIRMED, CANCELLED}

    static class Booking {
        private int id; private int packageId; private int customerId; private List<Traveler> travelers; private int numSeats; private BookingStatus status; private double amountDue; private double paidAmount; private LocalDate bookingDate; private List<Integer> paymentIds = new ArrayList<>();
        public Booking(int id,int packageId,int customerId,List<Traveler> travelers,int numSeats,double amountDue,LocalDate bookingDate){this.id=id;this.packageId=packageId;this.customerId=customerId;this.travelers=travelers;this.numSeats=numSeats;this.status=BookingStatus.PENDING;this.amountDue=amountDue;this.paidAmount=0;this.bookingDate=bookingDate;}
        public int getId(){return id;} public int getPackageId(){return packageId;} public int getNumSeats(){return numSeats;} public BookingStatus getStatus(){return status;} public void setStatus(BookingStatus s){status=s;} public double getAmountDue(){return amountDue;} public double getPaidAmount(){return paidAmount;} public void addPayment(Payment p){this.paidAmount += p.getAmount(); paymentIds.add(p.getId());}
    }

    static class Payment {
        private int id; private int bookingId; private double amount; private LocalDate date; private String method;
        public Payment(int id,int bookingId,double amount,LocalDate date,String method){this.id=id;this.bookingId=bookingId;this.amount=amount;this.date=date;this.method=method;} public int getId(){return id;} public double getAmount(){return amount;} public String toString(){return String.format("Payment %d: Booking %d Amount %.2f on %s via %s", id, bookingId, amount, date, method);}    }

    static class Cancellation {
        private int id; private int bookingId; private LocalDate cancelDate; private double fee; private double refund;
        public Cancellation(int id,int bookingId,LocalDate cancelDate,double fee,double refund){this.id=id;this.bookingId=bookingId;this.cancelDate=cancelDate;this.fee=fee;this.refund=refund;}    }
}
