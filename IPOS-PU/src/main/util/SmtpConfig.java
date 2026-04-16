package main.util;

/**
 * Reads SMTP settings from environment variables so secrets stay out of the repo.
 */
public record SmtpConfig(
        String host,
        int port,
        String username,
        String password,
        String from,
        boolean startTls,
        boolean ssl
) {
    public static SmtpConfig fromEnvironment() {
        String host = env("IPOS_SMTP_HOST");
        String username = env("IPOS_SMTP_USERNAME");
        String password = env("IPOS_SMTP_PASSWORD");
        String from = env("IPOS_SMTP_FROM");
        if (host == null || username == null || password == null || from == null) {
            return null;
        }
        int port = parsePort(env("IPOS_SMTP_PORT"), 587);
        boolean startTls = parseBoolean(env("IPOS_SMTP_STARTTLS"), true);
        boolean ssl = parseBoolean(env("IPOS_SMTP_SSL"), false);
        return new SmtpConfig(host, port, username, password, from, startTls, ssl);
    }

    private static String env(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static int parsePort(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
