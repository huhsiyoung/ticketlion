package site.ticketlion.web.exception;

public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException(String seatId) {
        super("Seat not found: " + seatId);
    }
}