package com.mycompany.groceryshop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/GroceryShop";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    // Establishes a connection to the database
    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    // Retrieves available quantity for a given product
    public int getAvailableQty(String productName) {
        String query = "SELECT qty FROM productlist WHERE productname = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, productName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("qty");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0; // If product not found
    }

    // Updates the quantity of a product after a purchase
    public void updateProductQty(String productName, int qty) {
        String query = "UPDATE productlist SET qty = qty - ? WHERE productname = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, qty);
            pstmt.setString(2, productName);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Adds a new product to the product list
    public void addProduct(String productName, int qty, double price) {
        String query = "INSERT INTO productlist (productname, qty, price) VALUES (?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, productName);
            pstmt.setInt(2, qty);
            pstmt.setDouble(3, price);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Deletes a product from the product list
    public void deleteProduct(String productName) {
        String query = "DELETE FROM productlist WHERE productname = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, productName);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
