package main.implementation;

import main.api.PUCommsAPI;
import main.db.DatabaseManager;
import main.util.SmtpConfig;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;

public class PUCommsAPIImpl implements PUCommsAPI {

    public PUCommsAPIImpl() {
    }

    @Override
    public boolean sendEmailFromSubsystem(String sourceSubsystem, String to, String subject, String body) {
        if (to == null || to.trim().isEmpty() || body == null || body.trim().isEmpty()) {
            System.out.println("[EMAIL] Failed: recipient or body is missing.");
            return false;
        }
        String src = sourceSubsystem == null || sourceSubsystem.isBlank() ? "IPOS-PU" : sourceSubsystem.trim();
        persistEmailAudit(src, to, subject, body);
        boolean delivered = trySendViaSmtp(src, to, subject, body);
        if (!delivered) {
            System.out.println("[EMAIL] Audit-only mode: SMTP not configured or send failed.");
            System.out.println("[EMAIL] From: " + src + " | To: " + to);
            System.out.println("[EMAIL] Subject: " + subject);
            System.out.println("[EMAIL] Body: " + body);
        }
        recordTransaction("EMAIL_" + System.currentTimeMillis(), "email",
                delivered ? "smtp-sent" : "audit-only", LocalDateTime.now().toString());
        return delivered || SmtpConfig.fromEnvironment() == null;
    }

    /**
     * Full payment authorisation with card number validation.
     * Validates orderId, amount, and card number before simulating authorisation. Card number must be at least 12 digits. Only the last 4 digits are shown in the output.
     */
    public boolean authorisePayment(String orderId, double amount, String cardNumber) {
        if (orderId == null || orderId.trim().isEmpty()) {
            System.out.println("[PAYMENT] Failed: orderId is null or empty.");
            persistPaymentAudit("IPOS-PU", orderId, null, amount, null, "FAILED_INVALID_ORDER");
            return false;
        }
        if (amount <= 0) {
            System.out.println("[PAYMENT] Failed: amount must be greater than 0.");
            persistPaymentAudit("IPOS-PU", orderId, null, amount, null, "FAILED_INVALID_AMOUNT");
            return false;
        }
        if (cardNumber == null || cardNumber.replaceAll("\\s", "").length() < 12) {
            System.out.println("[PAYMENT] Failed: card number is invalid.");
            persistPaymentAudit("IPOS-PU", orderId, null, amount, maskCard(cardNumber), "FAILED_INVALID_CARD");
            return false;
        }
        String digits = cardNumber.replaceAll("\\s", "");
        String masked = "**** **** **** " + digits.substring(digits.length() - 4);
        System.out.println("[PAYMENT] Authorised: Order " + orderId
                + " | Amount: £" + String.format("%.2f", amount)
                + " | Card: " + masked);
        persistPaymentAudit("IPOS-PU", orderId, null, amount, masked, "AUTHORISED");
        recordTransaction("PAY_" + orderId, "payment", "success",
                LocalDateTime.now().toString());
        return true;
    }

    @Override
    public boolean authorisePayment(String orderId, double amount) {
        return authorisePayment(orderId, amount, "000000000000");
    }

    @Override
    public void recordCardPaymentFromSubsystem(String sourceSubsystem, String orderId, String payeeEmail,
                                               double amount, String cardMasked, String outcome) {
        String src = sourceSubsystem == null || sourceSubsystem.isBlank() ? "UNKNOWN" : sourceSubsystem.trim();
        persistPaymentAudit(src, orderId, payeeEmail, amount, cardMasked, outcome);
        recordTransaction("EXT-PAY-" + (orderId != null ? orderId : "na"), "card-payment",
                outcome + "|subsystem=" + src, LocalDateTime.now().toString());
    }

    @Override
    public void recordTransaction(String refId, String type, String outcome, String timestamp) {
        System.out.println("[TRANSACTION] Ref: " + refId
                + " | Type: " + type
                + " | Outcome: " + outcome
                + " | Time: " + timestamp);
    }

    private static String maskCard(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }
        String digits = cardNumber.replaceAll("\\s", "");
        if (digits.length() < 4) {
            return "****";
        }
        return "**** **** **** " + digits.substring(digits.length() - 4);
    }

    private static void persistEmailAudit(String subsystem, String to, String subject, String body) {
        String sql = """
            INSERT INTO email_audit (subsystem, recipient, subject, body)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subsystem);
            ps.setString(2, to.trim());
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[EMAIL_AUDIT] Failed to persist: " + e.getMessage());
        }
    }

    private static void persistPaymentAudit(String subsystem, String orderId, String payeeEmail,
                                            double amount, String cardMasked, String outcome) {
        String sql = """
            INSERT INTO payment_audit (subsystem, order_id, payee_email, amount, card_masked, outcome)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subsystem);
            ps.setString(2, orderId);
            ps.setString(3, payeeEmail);
            ps.setDouble(4, amount);
            ps.setString(5, cardMasked);
            ps.setString(6, outcome);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[PAYMENT_AUDIT] Failed to persist: " + e.getMessage());
        }
    }

    private static boolean trySendViaSmtp(String subsystem, String to, String subject, String body) {
        SmtpConfig config = SmtpConfig.fromEnvironment();
        if (config == null) {
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", config.host());
        props.put("mail.smtp.port", String.valueOf(config.port()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.startTls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(config.ssl()));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.username(), config.password());
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.from()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.trim()));
            message.setSubject(subject == null ? ("Message from " + subsystem) : subject);
            message.setText(body, "UTF-8");
            Transport.send(message);
            System.out.println("[EMAIL] SMTP sent: " + subsystem + " -> " + to);
            return true;
        } catch (MessagingException ex) {
            System.err.println("[EMAIL] SMTP send failed: " + ex.getMessage());
            return false;
        }
    }
}
