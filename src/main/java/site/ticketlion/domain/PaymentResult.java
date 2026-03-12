package site.ticketlion.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentResult {
    private boolean success;
    private String transactionId; // 모의값
    private String message;
}
