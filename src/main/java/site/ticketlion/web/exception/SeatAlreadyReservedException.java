package site.ticketlion.web.exception;

public class SeatAlreadyReservedException extends RuntimeException {

    public SeatAlreadyReservedException(String seatId) {
        super("Seat already reserved: " + seatId);
    }
}
