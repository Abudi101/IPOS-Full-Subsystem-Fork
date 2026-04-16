package tests.implementation;

import main.db.DatabaseManager;
import main.implementation.CAMerchantStockAPIImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class CAMerchantStockAPIImplTest {

    private static final String SAMPLE_PRODUCT = "10000001";

    private CAMerchantStockAPIImpl api;

    @BeforeEach
    void setUp() {
        DatabaseManager.initialise();
        api = new CAMerchantStockAPIImpl();
    }

    @Test
    void testCheckStock_ReturnsExpectedValues() {
        assertTrue(api.checkStock(SAMPLE_PRODUCT) >= 0);
        assertEquals(-1, api.checkStock("NO-SUCH-PRODUCT"));
        assertEquals(-1, api.checkStock(null));
        assertEquals(-1, api.checkStock(" "));
    }

    @Test
    void testDeductStock_ValidQuantity_DecreasesStock() throws SQLException {
        int original = getStock(SAMPLE_PRODUCT);
        setStock(SAMPLE_PRODUCT, 30);
        try {
            boolean deducted = api.deductStock(SAMPLE_PRODUCT, 5);
            int after = getStock(SAMPLE_PRODUCT);

            assertTrue(deducted);
            assertEquals(25, after);
        } finally {
            setStock(SAMPLE_PRODUCT, original);
        }
    }

    @Test
    void testDeductStock_InvalidOrInsufficient_ReturnsFalse() throws SQLException {
        int original = getStock(SAMPLE_PRODUCT);
        setStock(SAMPLE_PRODUCT, 2);
        try {
            assertFalse(api.deductStock(SAMPLE_PRODUCT, 3));
            assertFalse(api.deductStock(SAMPLE_PRODUCT, 0));
            assertFalse(api.deductStock(" ", 1));
            assertFalse(api.deductStock(null, 1));
            assertEquals(2, getStock(SAMPLE_PRODUCT));
        } finally {
            setStock(SAMPLE_PRODUCT, original);
        }
    }

    @Test
    void testListAvailableStock_FilteringAndNoResults() {
        String allProducts = api.listAvailableStock(null);
        assertTrue(allProducts.contains(SAMPLE_PRODUCT));
        assertTrue(allProducts.contains("Stock:"));

        String filtered = api.listAvailableStock("Paracetamol");
        assertTrue(filtered.toLowerCase().contains("paracetamol"));

        String notFound = api.listAvailableStock("no-such-keyword-xyz");
        assertEquals("No products found.", notFound);
    }

    @Test
    void testSubmitPaidOrder_ReturnsExpectedMessages() {
        assertEquals("Invalid order ID.", api.submitPaidOrder(null, SAMPLE_PRODUCT + ":2"));
        assertEquals("Invalid order ID.", api.submitPaidOrder(" ", SAMPLE_PRODUCT + ":2"));
        assertEquals("No items supplied.", api.submitPaidOrder("ORD-1", null));
        assertEquals("No items supplied.", api.submitPaidOrder("ORD-1", " "));

        String result = api.submitPaidOrder("ORD-12345", SAMPLE_PRODUCT + ":2;10000002:1");
        assertTrue(result.contains("IPOS-CA"));
        assertTrue(result.contains("ORD-12345"));
    }

    private int getStock(String productId) throws SQLException {
        String sql = "SELECT stock FROM products WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt("stock");
            }
        }
    }

    private void setStock(String productId, int value) throws SQLException {
        String sql = "UPDATE products SET stock = ? WHERE product_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, value);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }
}
