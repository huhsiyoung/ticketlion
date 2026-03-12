package site.ticketlion.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private Long reservationId;
    private Long amount;
    private String status;
}