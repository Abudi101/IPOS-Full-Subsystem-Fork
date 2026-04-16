package main.api;

import java.time.LocalDateTime;

public interface PUCommsAPI {

    default boolean sendEmail(String to, String subject, String body) {
        return sendEmailFromSubsystem("IPOS-PU", to, subject, body);
    }

    /**
     * @param sourceSubsystem e.g. "IPOS-PU", "IPOS-SA", "IPOS-CA" (brief: comms shared across subsystems).
     */
    boolean sendEmailFromSubsystem(String sourceSubsystem, String to, String subject, String body);

    boolean authorisePayment(String orderId, double amount);

    void recordTransaction(String refId, String type, String outcome, String timestamp);

    /**
     * Records a card payment audit trail for payments originating from another subsystem (brief IPOS-PU-COMMS).
     */
    default void recordCardPaymentFromSubsystem(String sourceSubsystem, String orderId, String payeeEmail,
                                                double amount, String cardMasked, String outcome) {
        recordTransaction("EXT-PAY-" + (orderId != null ? orderId : "na"), "card-payment",
                outcome + "|subsystem=" + sourceSubsystem + "|amount=" + amount + "|payee=" + payeeEmail
                        + "|card=" + cardMasked,
                LocalDateTime.now().toString());
    }
}
