package Chart;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.SwingWrapper;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import Database.DatabaseManager;
import Database.UserSession;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class IncomeExpenseChart {

    // Array of month names in chronological order
    private static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June", "July", "August", "September", "October",
            "November", "December"
    };

    public static Map<String, Integer> getMonthlyIncome() throws SQLException {
        Map<String, Integer> monthlyIncome = new TreeMap<>();
        DatabaseManager.connect();
        String query = "SELECT monthname(income_date) AS month, SUM(amount) AS income " +
                "FROM income " +
                "WHERE user_id = ? " +
                "GROUP BY month";
        try (PreparedStatement pstmt = DatabaseManager.getConnection().prepareStatement(query)) {
            pstmt.setInt(1, UserSession.userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String month = rs.getString("month");
                int income = rs.getInt("income");
                monthlyIncome.put(month, income);
            }
        }
        // Ensure all months are in the map with 0 income if no records found
        for (String month : MONTHS) {
            monthlyIncome.putIfAbsent(month, 0);
        }
        return monthlyIncome;
    }

    public static Map<String, Integer> getMonthlyExpenses() throws SQLException {
        Map<String, Integer> monthlyExpenses = new TreeMap<>();
        DatabaseManager.connect();
        String query = "SELECT monthname(expense_date) AS month, SUM(amount) AS expense " +
                "FROM expense " +
                "WHERE user_id = ? " +
                "GROUP BY month";
        try (PreparedStatement pstmt = DatabaseManager.getConnection().prepareStatement(query)) {
            pstmt.setInt(1, UserSession.userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String month = rs.getString("month");
                int expense = rs.getInt("expense");
                monthlyExpenses.put(month, expense);
            }
        }
        // Ensure all months are in the map with 0 expenses if no records found
        for (String month : MONTHS) {
            monthlyExpenses.putIfAbsent(month, 0);
        }
        return monthlyExpenses;
    }

    public static Map<String, Integer> getIncomeCategories() throws SQLException {
        Map<String, Integer> incomeCategories = new HashMap<>();
        DatabaseManager.connect();
        String query = "SELECT income_source, SUM(amount) AS total " +
                "FROM income " +
                "WHERE user_id = ? " +
                "GROUP BY income_source";
        try (PreparedStatement pstmt = DatabaseManager.getConnection().prepareStatement(query)) {
            pstmt.setInt(1, UserSession.userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String category = rs.getString("income_source");
                int amount = rs.getInt("total");
                incomeCategories.put(category, amount);
            }
        }
        return incomeCategories;
    }

    public static Map<String, Integer> getExpenseCategories() throws SQLException {
        Map<String, Integer> expenseCategories = new HashMap<>();
        DatabaseManager.connect();
        String query = "SELECT expense_category, SUM(amount) AS total " +
                "FROM expense " +
                "WHERE user_id = ? " +
                "GROUP BY expense_category";
        try (PreparedStatement pstmt = DatabaseManager.getConnection().prepareStatement(query)) {
            pstmt.setInt(1, UserSession.userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String category = rs.getString("expense_category");
                int amount = rs.getInt("total");
                expenseCategories.put(category, amount);
            }
        }
        return expenseCategories;
    }

    public static void generateLineChart(Map<String, Integer> monthlyIncome, Map<String, Integer> monthlyExpenses) {
        Thread chartThread = new Thread(() -> {
            // Create XChart
            CategoryChart chart = new CategoryChartBuilder()
                    .width(1000)
                    .height(504)
                    .title("Monthly Income vs. Expenses")
                    .xAxisTitle("Month")
                    .yAxisTitle("Amount")
                    .build();

            // Add income and expense series to the chart
            chart.addSeries("Income", new ArrayList<>(monthlyIncome.keySet()), new ArrayList<>(monthlyIncome.values()));
            chart.addSeries("Expenses", new ArrayList<>(monthlyExpenses.keySet()),
                    new ArrayList<>(monthlyExpenses.values()));

            SwingWrapper<CategoryChart> wrapper = new SwingWrapper<>(chart);
            JFrame frame = wrapper.displayChart();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        });
        chartThread.start();
    }

    public static void generatePieChart(Map<String, Integer> categories, String title) {
        Thread chartThread = new Thread(() -> {
            // Create XChart
            PieChart chart = new PieChartBuilder()
                    .width(800)
                    .height(600)
                    .title(title)
                    .build();

            // Add series to the chart
            for (Map.Entry<String, Integer> entry : categories.entrySet()) {
                chart.addSeries(entry.getKey(), entry.getValue());
            }

            SwingWrapper<PieChart> wrapper = new SwingWrapper<>(chart);
            JFrame frame = wrapper.displayChart();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        });
        chartThread.start();
    }

    public static void showCharts() {
        try {
            // Get data for line chart
            Map<String, Integer> monthlyIncome = getMonthlyIncome();
            Map<String, Integer> monthlyExpenses = getMonthlyExpenses();
            
            // Generate line chart
            generateLineChart(monthlyIncome, monthlyExpenses);

            // Get data for pie charts
            Map<String, Integer> incomeCategories = getIncomeCategories();
            Map<String, Integer> expenseCategories = getExpenseCategories();

            // Generate pie charts
            generatePieChart(incomeCategories, "Income Distribution by Category");
            generatePieChart(expenseCategories, "Expense Distribution by Category");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error generating charts: " + e.getMessage());
        }
    }
}