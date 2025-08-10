package com.mkso4ka.mindustry.matrixproc;

import arc.util.Log;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Кастомный логгер, который запускает локальный веб-сервер для отображения логов в реальном времени.
 */
public class WebLogger {

    /**
     * ГЛАВНАЯ КОНСТАНТА: Установите false, чтобы полностью отключить веб-сервер и логгирование через него.
     * При значении false компилятор вырежет весь связанный код.
     */
    public static final boolean ENABLE_WEB_LOGGER = true;

    private static final int PORT = 8080;
    private static final List<String> logs = new ArrayList<>();
    private static HttpServer server;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private static void log(String level, String text, Object... args) {
        // 1. Форматируем сообщение
        String formattedText = args.length == 0 ? text : String.format(text, args);
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, formattedText);

        // 2. Дублируем в стандартный логгер Mindustry
        if (level.equals("E")) Log.err(formattedText);
        else if (level.equals("W")) Log.warn(formattedText);
        else Log.info(formattedText);

        // 3. Если веб-логгер включен, добавляем в наш список
        if (ENABLE_WEB_LOGGER && server != null) {
            synchronized (logs) {
                logs.add(logEntry);
            }
        }
    }

    public static void info(String text, Object... args) {
        log("I", text, args);
    }

    public static void warn(String text, Object... args) {
        log("W", text, args);
    }

    public static void err(String text, Object... args) {
        log("E", text, args);
    }

    public static void startServer() {
        if (!ENABLE_WEB_LOGGER || server != null) {
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Обработчик для Web UI
            server.createContext("/", exchange -> {
                String html = "<html><head><title>PictureToLogic Logs</title>" +
                    "<style>body{font-family:monospace;background-color:#2c2c2c;color:#e0e0e0;} pre{white-space:pre-wrap;word-break:break-all;} button{padding:10px;}</style></head>" +
                    "<body><h1>PictureToLogic Live Logs</h1><button onclick='clearLogs()'>Clear Logs</button><pre id='logs'></pre>" +
                    "<script>" +
                    "const logsPre = document.getElementById('logs');" +
                    "function fetchLogs(){ fetch('/logs').then(res => res.text()).then(text => { logsPre.textContent = text; logsPre.scrollTop = logsPre.scrollHeight; }); }" +
                    "function clearLogs(){ fetch('/logs', { method: 'DELETE' }).then(() => fetchLogs()); }" +
                    "setInterval(fetchLogs, 2000);" +
                    "fetchLogs();" +
                    "</script></body></html>";
                sendResponse(exchange, 200, html, "text/html");
            });

            // Обработчик для API (Termux)
            server.createContext("/logs", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String response;
                    synchronized (logs) {
                        response = String.join("\n", logs);
                    }
                    sendResponse(exchange, 200, response, "text/plain");
                } else if ("DELETE".equals(exchange.getRequestMethod())) {
                    synchronized (logs) {
                        logs.clear();
                    }
                    sendResponse(exchange, 200, "Logs cleared.", "text/plain");
                } else {
                    sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                }
            });

            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            Log.info("[green]WebLogger запущен на http://localhost:" + PORT + "[/]");

            // Добавляем хук для корректной остановки сервера при выходе из игры
            Runtime.getRuntime().addShutdownHook(new Thread(WebLogger::stopServer));

        } catch (IOException e) {
            Log.err("Не удалось запустить WebLogger", e);
        }
    }

    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
            Log.info("[scarlet]WebLogger остановлен.[/]");
        }
    }

    private static void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int code, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}