package site.ticketlion.web.dto.request;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Long eventId;
    private List<String> seatNumbers;
    private Long reservationId;
    private String paymentMethod;
}