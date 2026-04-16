package main.service;

import main.db.DatabaseManager;
import main.model.User;

import java.security.SecureRandom;
import java.sql.*;

public class AuthService {

    public User login(String email, String password) {
        if (email == null || password == null) return null;

        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;
            if (!rs.getString("password").equals(password)) return null;

            return new User(
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getInt("first_login") == 1,
                    rs.getString("full_name")
            );

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean changePassword(User user, String newPassword) {
        if (user == null || newPassword == null || newPassword.trim().isEmpty()) return false;
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{10,}$")) return false;
        //        if (newPassword.length() < 6) return false;

        String sql = "UPDATE users SET password = ?, first_login = 0 WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setString(2, user.getEmail());
            ps.executeUpdate();

            user.setPassword(newPassword);
            user.setFirstLogin(false);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String registerNonCommercialMember(String fullName, String email) {
        if (fullName == null || fullName.trim().isEmpty()) return null;
        if (email == null || email.trim().isEmpty()) return null;

        String cleanEmail = email.trim().toLowerCase();
        if (emailExists(cleanEmail)) return null;

        String tempPassword = generateNonCommercialTempPassword();
        String sql = """
            INSERT INTO users (email, full_name, password, role, first_login)
            VALUES (?, ?, ?, 'CUSTOMER', 1)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cleanEmail);
            ps.setString(2, fullName.trim());
            ps.setString(3, tempPassword);
            ps.executeUpdate();
            return tempPassword;

        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Brief 8.3: random string of 10 symbols including letters, numbers, and special characters.
     */
    public static String generateNonCommercialTempPassword() {
        final String letters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
        final String digits = "23456789";
        final String special = "!@#$%&*?";
        final String all = letters + digits + special;
        SecureRandom r = new SecureRandom();
        char[] pwd = new char[10];
        pwd[0] = letters.charAt(r.nextInt(letters.length()));
        pwd[1] = digits.charAt(r.nextInt(digits.length()));
        pwd[2] = special.charAt(r.nextInt(special.length()));
        for (int i = 3; i < pwd.length; i++) {
            pwd[i] = all.charAt(r.nextInt(all.length()));
        }
        for (int i = pwd.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            char t = pwd[i];
            pwd[i] = pwd[j];
            pwd[j] = t;
        }
        return new String(pwd);
    }

    public boolean emailExists(String email) {
        if (email == null || email.trim().isEmpty()) return false;

        String sql = "SELECT 1 FROM users WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            return ps.executeQuery().next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}