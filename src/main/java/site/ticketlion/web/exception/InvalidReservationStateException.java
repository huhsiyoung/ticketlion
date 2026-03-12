package site.ticketlion.web.exception;

public class InvalidReservationStateException extends RuntimeException {
    public InvalidReservationStateException(String msg) {
        super(msg);
    }
}
