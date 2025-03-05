import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ForeignTradingGUI {
    private JFrame frame;
    private JTextField usernameField, passwordField;
    private JLabel balanceLabel;
    private JTable transactionTable;
    private DefaultTableModel tableModel;
    private Connection conn;
    private int loggedInUserId = -1;

    public ForeignTradingGUI() {
        connectDatabase();
        createLoginScreen();
    }

    private void connectDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/foreign_trading", "root", "pass123");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Connection Failed!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void createLoginScreen() {
        frame = new JFrame("Foreign Trading System - Login");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(4, 1));
        frame.getContentPane().setBackground(new Color(173, 216, 230));

        JPanel panel = new JPanel();
        panel.setBackground(new Color(173, 216, 230));
        panel.add(new JLabel("Username: "));
        usernameField = new JTextField(15);
        panel.add(usernameField);

        JPanel panel2 = new JPanel();
        panel2.setBackground(new Color(173, 216, 230));
        panel2.add(new JLabel("Password: "));
        passwordField = new JPasswordField(15);
        panel2.add(passwordField);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        loginButton.setBackground(new Color(30, 144, 255));
        loginButton.setForeground(Color.WHITE);
        registerButton.setBackground(new Color(60, 179, 113));
        registerButton.setForeground(Color.WHITE);

        loginButton.addActionListener(e -> loginUser());
        registerButton.addActionListener(e -> registerUser());

        frame.add(panel);
        frame.add(panel2);
        frame.add(loginButton);
        frame.add(registerButton);
        frame.setVisible(true);
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String password = new String(((JPasswordField) passwordField).getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Fields cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "INSERT INTO users (username, password, balance) VALUES (?, ?, 1000.0)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Registration Successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loginUser() {
        String username = usernameField.getText().trim();
        String password = new String(((JPasswordField) passwordField).getPassword());

        String sql = "SELECT id FROM users WHERE username=? AND password=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                loggedInUserId = rs.getInt("id");
                frame.dispose();
                createDashboard();
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void updateBalance() {
        if (loggedInUserId == -1) return;

        String sql = "SELECT balance FROM users WHERE id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, loggedInUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                balanceLabel.setText("Balance: $" + balance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void updateTransactionHistory() {
        tableModel.setRowCount(0);
        String sql = "SELECT transaction_type, currency, amount, price, timestamp FROM transactions WHERE user_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, loggedInUserId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getString("transaction_type"), rs.getString("currency"), rs.getDouble("amount"), rs.getDouble("price"), rs.getTimestamp("timestamp")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void performTransaction(String type) {
        String currency = JOptionPane.showInputDialog(frame, "Enter Currency (USD, EUR, JPY,INR):");
        if (currency == null || currency.trim().isEmpty()) return;

        String amountStr = JOptionPane.showInputDialog(frame, "Enter Amount:");
        if (amountStr == null || amountStr.trim().isEmpty()) return;

        String priceStr = JOptionPane.showInputDialog(frame, "Enter Price per Unit:");
        if (priceStr == null || priceStr.trim().isEmpty()) return;

        try {
            double amount = Double.parseDouble(amountStr);
            double price = Double.parseDouble(priceStr);
            double totalCost = amount * price;

            String balanceQuery = "SELECT balance FROM users WHERE id=?";
            String updateBalanceQuery = "UPDATE users SET balance=? WHERE id=?";
            String transactionQuery = "INSERT INTO transactions (user_id, currency, amount, price, transaction_type) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(balanceQuery)) {
                stmt.setInt(1, loggedInUserId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    double balance = rs.getDouble("balance");

                    if (type.equals("BUY") && balance < totalCost) {
                        JOptionPane.showMessageDialog(frame, "Insufficient balance!", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    double newBalance = type.equals("BUY") ? balance - totalCost : balance + totalCost;
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateBalanceQuery)) {
                        updateStmt.setDouble(1, newBalance);
                        updateStmt.setInt(2, loggedInUserId);
                        updateStmt.executeUpdate();
                    }

                    try (PreparedStatement transStmt = conn.prepareStatement(transactionQuery)) {
                        transStmt.setInt(1, loggedInUserId);
                        transStmt.setString(2, currency);
                        transStmt.setDouble(3, amount);
                        transStmt.setDouble(4, price);
                        transStmt.setString(5, type);
                        transStmt.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(frame, type + " Successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    updateBalance();
                    updateTransactionHistory();
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid input!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void createDashboard() {
        frame = new JFrame("Foreign Trading System - Dashboard");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(240, 248, 255));


        balanceLabel = new JLabel("Balance: Loading...");
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(balanceLabel, BorderLayout.NORTH);
        updateBalance();


        JPanel panel = new JPanel();
        panel.setBackground(new Color(240, 248, 255));
        JButton buyButton = new JButton("Buy Currency");
        JButton sellButton = new JButton("Sell Currency");
        JButton backButton = new JButton("Back");

        buyButton.setBackground(new Color(34, 139, 34));
        buyButton.setForeground(Color.WHITE);
        sellButton.setBackground(new Color(178, 34, 34));
        sellButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(70, 130, 180));
        backButton.setForeground(Color.WHITE);


        buyButton.addActionListener(e -> performTransaction("BUY"));
        sellButton.addActionListener(e -> performTransaction("SELL"));
        backButton.addActionListener(e -> {
            frame.dispose();
            createLoginScreen();
        });

        panel.add(buyButton);
        panel.add(sellButton);
        panel.add(backButton);
        frame.add(panel, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(new String[]{"Type", "Currency", "Amount", "Price", "Timestamp"}, 0);
        transactionTable = new JTable(tableModel);
        frame.add(new JScrollPane(transactionTable), BorderLayout.SOUTH);
        updateTransactionHistory();

        frame.setVisible(true);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ForeignTradingGUI::new);
    }
}
