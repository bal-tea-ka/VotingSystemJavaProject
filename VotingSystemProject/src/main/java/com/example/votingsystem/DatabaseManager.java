package com.example.votingsystem;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:voting_system.db";

    public DatabaseManager() {
        initializeDatabase();
    }

    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    private void initializeDatabase() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "role TEXT NOT NULL DEFAULT 'elector')";
        String createCandidatesTable = "CREATE TABLE IF NOT EXISTS candidates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL)";
        String createVotesTable = "CREATE TABLE IF NOT EXISTS votes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "candidate_id INTEGER," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "FOREIGN KEY (candidate_id) REFERENCES candidates(id))";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createCandidatesTable);
            stmt.execute(createVotesTable);
            // Добавим тестового админа
            stmt.execute("INSERT OR IGNORE INTO users (username, password, role) VALUES ('admin', 'admin123', 'admin')");

        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }

    public String checkUser(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role"); // Возвращаем роль
            }
            return null;
        } catch (SQLException e) {
            System.out.println("Error checking user: " + e.getMessage());
            return null;
        }
    }

    public boolean addUser(String username, String password, String role) {
        String sql = "INSERT INTO users(username, password, role) VALUES(?, ?, ?)";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error adding user: " + e.getMessage());
            return false;
        }
    }

    public boolean addCandidate(String name) {
        String decodedName;
        try {
            decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            // Если декодирование не удалось, используем исходное значение
            decodedName = name;
            System.out.println("Ошибка декодирования: " + e.getMessage());
        }
        name = decodedName;
        String sql = "INSERT INTO candidates(name) VALUES(?)";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public String getCandidates() {
        String sql = "SELECT c.id, c.name, COUNT(v.id) as votes FROM candidates c " +
                "LEFT JOIN votes v ON c.id = v.candidate_id GROUP BY c.id, c.name";
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{\"id\": ").append(rs.getInt("id"))
                        .append(", \"name\": \"").append(rs.getString("name"))
                        .append("\", \"votes\": ").append(rs.getInt("votes")).append("}");
                first = false;
            }
            json.append("]");
            return json.toString();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return "[]";
        }
    }

    public String getResults() {
        String sql = "SELECT c.name, COUNT(v.id) as votes FROM candidates c " +
                "LEFT JOIN votes v ON c.id = v.candidate_id GROUP BY c.id, c.name";
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{\"name\": \"").append(rs.getString("name"))
                        .append("\", \"votes\": ").append(rs.getInt("votes")).append("}");
                first = false;
            }
            json.append("]");
            return json.toString();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return "[]";
        }
    }

    // Новый метод: добавление голоса
    public boolean addVote(int userId, int candidateId) {
        String sql = "INSERT INTO votes(user_id, candidate_id) VALUES(?, ?)";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, candidateId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false; // Например, пользователь уже голосовал
        }
    }

    // Новый метод: получение ID пользователя по имени
    public int getUserId(String name) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return -1; // Пользователь не найден
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return -1;
        }
    }
}