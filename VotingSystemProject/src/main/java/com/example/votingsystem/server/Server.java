package com.example.votingsystem.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.example.votingsystem.DatabaseManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 8080;
    private static final DatabaseManager dbManager = new DatabaseManager();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/candidates", new CandidatesHandler());
        server.createContext("/api/vote", new VoteHandler());
        server.createContext("/api/add-candidate", new AddCandidateHandler()); // Перемещено сюда
        server.createContext("/api/results", new ResultsHandler()); // Перемещено сюда
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    // Обработчик статических файлов
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                // Получаем путь из запроса (например, /index.html, /styles.css, /user.html)
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/") || path.equals("")) {
                    path = "/index.html"; // Если запрос к корню, возвращаем index.html
                }

                // Убираем начальный слэш и добавляем префикс "static/"
                String resourcePath = "static" + path;
                InputStream inputStream = Server.class.getClassLoader().getResourceAsStream(resourcePath);

                if (inputStream == null) {
                    sendResponse(exchange, 404, "{\"error\": \"File not found\"}");
                    return;
                }

                // Определяем Content-Type в зависимости от расширения файла
                String contentType = "text/plain";
                if (path.endsWith(".html")) {
                    contentType = "text/html";
                } else if (path.endsWith(".css")) {
                    contentType = "text/css";
                } else if (path.endsWith(".js")) {
                    contentType = "application/javascript";
                }

                // Читаем содержимое файла
                byte[] responseBytes = inputStream.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Добавляем CORS
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Регистрация пользователя
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                Map<String, String> params = parsePostParams(exchange);
                String username = params.get("username");
                String password = params.get("password");
                String role = params.getOrDefault("role", "elector"); // По умолчанию elector

                if (dbManager.addUser(username, password, role)) {
                    sendResponse(exchange, 200, "{\"message\": \"User registered successfully\"}");
                } else {
                    sendResponse(exchange, 400, "{\"error\": \"User already exists\"}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Логин пользователя
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                Map<String, String> params = parsePostParams(exchange);
                String username = params.get("username");
                String password = params.get("password");
                System.out.println("Login attempt: username=" + username + ", password=" + password);

                String role = dbManager.checkUser(username, password);
                if (role != null) {
                    System.out.println("Login successful for: " + username + " with role: " + role);
                    sendResponse(exchange, 200, "{\"message\": \"Login successful\", \"role\": \"" + role + "\"}");
                } else {
                    System.out.println("Login failed for: " + username);
                    sendResponse(exchange, 401, "{\"error\": \"Invalid credentials\"}");
                }
            } else {
                System.out.println("Method not allowed for /api/login: " + exchange.getRequestMethod());
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Получение списка кандидатов
    static class CandidatesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = dbManager.getCandidates();
                sendResponse(exchange, 200, response);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Голосование
    static class VoteHandler implements HttpHandler {
        public VoteHandler() {
            // Пустой конструктор
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                Map<String, String> params = parsePostParams(exchange);
                String username = params.get("username");
                String candidateIdStr = params.get("candidateId");

                if (username == null || candidateIdStr == null) {
                    sendResponse(exchange, 400, "{\"error\": \"Missing username or candidateId\"}");
                    return;
                }

                try {
                    int candidateId = Integer.parseInt(candidateIdStr);
                    int userId = dbManager.getUserId(username);
                    if (userId != -1 && dbManager.addVote(userId, candidateId)) {
                        sendResponse(exchange, 200, "{\"message\": \"Vote recorded\"}");
                    } else {
                        sendResponse(exchange, 400, "{\"error\": \"Vote failed or user not found\"}");
                    }
                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "{\"error\": \"Invalid candidateId format\"}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Добавление кандидата
    static class AddCandidateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                Map<String, String> params = parsePostParams(exchange);
                String name = params.get("name");

                if (name != null && dbManager.addCandidate(name)) {
                    sendResponse(exchange, 200, "{\"message\": \"Candidate added successfully\"}");
                } else {
                    sendResponse(exchange, 400, "{\"error\": \"Failed to add candidate\"}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Получение результатов
    static class ResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = dbManager.getResults();
                sendResponse(exchange, 200, response);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // Вспомогательные методы
    private static byte[] fileToBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(bytes);
        }
        return bytes;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Разрешаем запросы с любого домена
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static Map<String, String> parsePostParams(HttpExchange exchange) throws IOException {
        String requestBody;
        try (InputStream is = exchange.getRequestBody()) {
            requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, String> params = new HashMap<>();
        for (String param : requestBody.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }
}