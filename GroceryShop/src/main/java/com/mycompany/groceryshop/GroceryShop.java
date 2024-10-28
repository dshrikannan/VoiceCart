package com.mycompany.groceryshop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;

public class GroceryShop extends JFrame {
    private JButton readOrderButton, updateStocksButton, addProductButton, updateProductButton, deleteProductButton,
            voiceButton;
    private JLabel currentDateLabel;
    private JPanel stocksPanel;
    private boolean stocksVisible = false;

    public GroceryShop() {
        // Initialize GUI components
        setTitle("Grocery Shop Billing System");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        CustomPanel mainPanel = new CustomPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));

        // Top panel for title and date
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        updateStocksButton = new JButton("Update Stocks");
        currentDateLabel = new JLabel("Current Date: " + LocalDate.now());
        topPanel.add(updateStocksButton);
        topPanel.add(currentDateLabel);

        // Center panel for welcome message
        JPanel centerPanel = new JPanel();
        JLabel welcomeLabel = new JLabel("KarthickShri Mart Welcomes You !!!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(Color.BLUE);
        centerPanel.add(welcomeLabel);

        // Bottom panel for buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        readOrderButton = new JButton("Read the Order");
        voiceButton = new JButton("Activate Voice Recognition");
        bottomPanel.add(readOrderButton);
        bottomPanel.add(voiceButton);

        // Stocks panel
        stocksPanel = new JPanel();
        stocksPanel.setLayout(new GridLayout(0, 1, 10, 10));
        stocksPanel.setVisible(false);
        addProductButton = new JButton("Add Stocks");
        updateProductButton = new JButton("Update Stocks");
        deleteProductButton = new JButton("Delete Stocks");
        stocksPanel.add(addProductButton);
        stocksPanel.add(updateProductButton);
        stocksPanel.add(deleteProductButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.add(stocksPanel, BorderLayout.EAST);

        // Action listeners
        updateStocksButton.addActionListener(e -> toggleStocksPanel());
        readOrderButton.addActionListener(e -> readOrderFromFile("order.txt"));
        addProductButton.addActionListener(e -> addProduct());
        updateProductButton.addActionListener(e -> updateProduct());
        deleteProductButton.addActionListener(e -> deleteProduct());

        // Voice recognition button action
        voiceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activateVoiceRecognition();
            }
        });

        setContentPane(mainPanel);
        setVisible(true);
    }

    private class CustomPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(new Color(255, 255, 204));
        }
    }

    private void toggleStocksPanel() {
        stocksVisible = !stocksVisible;
        stocksPanel.setVisible(stocksVisible);
        stocksPanel.revalidate();
        stocksPanel.repaint();
    }

    private void readOrderFromFile(String filePath) {
        StringBuilder products = new StringBuilder();
        double totalPrice = 0.0;
        StringBuilder unavailableProducts = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                String productName = parts[0];
                int qty = Integer.parseInt(parts[1]);

                if (checkProductAvailability(productName, qty)) {
                    double price = getProductPrice(productName);
                    totalPrice += price * qty;
                    products.append(productName).append(" ").append(qty).append("\n");
                } else {
                    unavailableProducts.append(productName).append("\n");
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading the order file: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (products.length() > 0) {
            String customerName = JOptionPane.showInputDialog("Enter Customer Name:");
            if (customerName != null && !customerName.trim().isEmpty()) {
                displayBill(customerName, products.toString(), totalPrice);
            }
        }

        if (unavailableProducts.length() > 0) {
            JOptionPane.showMessageDialog(this, "Unavailable Products:\n" + unavailableProducts.toString(),
                    "Unavailable Products", JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean checkProductAvailability(String productName, int qty) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/GroceryShop", "root",
                "password");
                PreparedStatement pstmt = conn.prepareStatement("SELECT qty FROM productlist WHERE productname = ?")) {
            pstmt.setString(1, productName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("qty") >= qty;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private double getProductPrice(String productName) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/GroceryShop", "root",
                "password");
                PreparedStatement pstmt = conn
                        .prepareStatement("SELECT price FROM productlist WHERE productname = ?")) {
            pstmt.setString(1, productName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("price");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private void displayBill(String customerName, String products, double totalPrice) {
        StringBuilder billDetails = new StringBuilder();
        billDetails.append("Bill Summary:\n");
        billDetails.append("Customer Name: ").append(customerName).append("\n");
        billDetails.append("Products:\n").append(products).append("\n");
        billDetails.append("Total Price: $").append(totalPrice).append("\n");

        int response = JOptionPane.showConfirmDialog(this, billDetails.toString(), "Confirm Order",
                JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
            placeOrder(customerName, products, totalPrice);
        } else {
            JOptionPane.showMessageDialog(this, "Order cancelled.");
        }
    }

    private void placeOrder(String customerName, String products, double totalPrice) {
        String[] productLines = products.split("\n");
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/GroceryShop", "root",
                "password")) {
            for (String line : productLines) {
                String[] productDetails = line.split(" ");
                String productName = productDetails[0];
                int qtyOrdered = Integer.parseInt(productDetails[1]);

                String updateStockQuery = "UPDATE productlist SET qty = qty - ? WHERE productname = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateStockQuery)) {
                    pstmt.setInt(1, qtyOrdered);
                    pstmt.setString(2, productName);
                    pstmt.executeUpdate();
                }
            }

            String insertOrderQuery = "INSERT INTO orderhistory (customername, list_of_products, total_price) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderQuery)) {
                pstmt.setString(1, customerName);
                pstmt.setString(2, products);
                pstmt.setDouble(3, totalPrice);
                pstmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Order placed successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error placing the order. Please try again.");
        }
    }

    private void addProduct() {
        String productName = JOptionPane.showInputDialog("Enter Product Name:");
        // Check if the user closed the dialog
        if (productName == null)
            return; // Exit if canceled

        String qtyString = JOptionPane.showInputDialog("Enter Quantity:");
        if (qtyString == null)
            return; // Exit if canceled

        String priceString = JOptionPane.showInputDialog("Enter Price:");
        if (priceString == null)
            return; // Exit if canceled

        int qty = Integer.parseInt(qtyString);
        double price = Double.parseDouble(priceString);

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/GroceryShop", "root",
                "password");
                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO productlist (productname, qty, price) VALUES (?, ?, ?)")) {
            pstmt.setString(1, productName);
            pstmt.setInt(2, qty);
            pstmt.setDouble(3, price);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Product added successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding product.");
        }
    }

    private void updateProduct() {
        String productName = JOptionPane.showInputDialog("Enter Product Name to Update:");
        if (productName == null)
            return; // Exit if canceled

        String newQtyString = JOptionPane.showInputDialog("Enter New Quantity:");
        if (newQtyString == null)
            return; // Exit if canceled

        String newPriceString = JOptionPane.showInputDialog("Enter New Price:");
        if (newPriceString == null)
            return; // Exit if canceled

        int newQty = Integer.parseInt(newQtyString);
        double newPrice = Double.parseDouble(newPriceString);

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/GroceryShop", "root",
                "password");
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE productlist SET qty = ?, price = ? WHERE productname = ?")) {
            pstmt.setInt(1, newQty);
            pstmt.setDouble(2, newPrice);
            pstmt.setString(3, productName);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "Product updated successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Product not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating product.");
        }
    }

    private void deleteProduct() {
        String productName = JOptionPane.showInputDialog("Enter Product Name to Delete:");
        if (productName == null)
            return; // Exit if canceled

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/GroceryShop", "root",
                "password");
                PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM productlist WHERE productname = ?")) {
            pstmt.setString(1, productName);
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                JOptionPane.showMessageDialog(this, "Product deleted successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Product not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting product.");
        }
    }

    private void activateVoiceRecognition() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_WINDOWS);
            robot.keyPress(KeyEvent.VK_H);
            robot.keyRelease(KeyEvent.VK_H);
            robot.keyRelease(KeyEvent.VK_WINDOWS);

            JOptionPane.showMessageDialog(this, "Please dictate your text. It will be saved in order.txt.");

            Timer timer = new Timer(5000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String textToAppend = JOptionPane.showInputDialog("Please enter the recognized text:");
                    if (textToAppend != null && !textToAppend.trim().isEmpty()) {
                        appendToFile(textToAppend);
                    } else {
                        JOptionPane.showMessageDialog(GroceryShop.this, "No text entered.");
                    }
                }
            });
            timer.setRepeats(false);
            timer.start();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(this, "Error initiating voice recognition: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void appendToFile(String textToAppend) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("order.txt", true))) {
            writer.write(textToAppend);
            writer.newLine();
            JOptionPane.showMessageDialog(this, "Text appended to order.txt.");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error appending to file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        new GroceryShop();
    }
}