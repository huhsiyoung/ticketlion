package site.ticketlion.web.exception;

public class SeatNotReservedException extends RuntimeException {
    public SeatNotReservedException(String seatNo) {
        super("Seat not reserved: " + seatNo);
    }
}
