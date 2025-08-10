package com.mkso4ka.mindustry.matrixproc;

import arc.util.Log;
import fi.iki.elonen.NanoHTTPD; // Импортируем NanoHTTPD
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Кастомный логгер, который запускает локальный веб-сервер для отображения логов в реальном времени.
 * ИСПОЛЬЗУЕТ NanoHTTPD для совместимости с Android.
 */
public class WebLogger extends NanoHTTPD { // Наследуемся от NanoHTTPD

    /**
     * ГЛАВНАЯ КОНСТАНТА: Установите false, чтобы полностью отключить веб-сервер и логгирование через него.
     */
    public static final boolean ENABLE_WEB_LOGGER = true;

    private static final int PORT = 8080;
    private static final List<String> logs = new ArrayList<>();
    private static WebLogger serverInstance;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    // Приватный конструктор, чтобы управлять созданием сервера
    private WebLogger() throws IOException {
        super(PORT);
    }

    // Основной метод обработки запросов
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        // Запрос на Web UI
        if ("/".equals(uri)) {
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
            return newFixedLengthResponse(Response.Status.OK, "text/html", html);
        }

        // Запрос на API (для Termux)
        if ("/logs".equals(uri)) {
            if (Method.GET.equals(method)) {
                String response;
                synchronized (logs) {
                    response = String.join("\n", logs);
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", response);
            } else if (Method.DELETE.equals(method)) {
                synchronized (logs) {
                    logs.clear();
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Logs cleared.");
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    // --- Публичные методы для логгирования ---

    private static void log(String level, String text, Object... args) {
        String formattedText = args.length == 0 ? text : String.format(text, args);
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, formattedText);

        if (level.equals("E")) Log.err(formattedText);
        else if (level.equals("W")) Log.warn(formattedText);
        else Log.info(formattedText);

        if (ENABLE_WEB_LOGGER && serverInstance != null) {
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

    // --- Управление сервером ---

    public static void startServer() {
        if (!ENABLE_WEB_LOGGER || serverInstance != null) {
            return;
        }
        try {
            serverInstance = new WebLogger();
            serverInstance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            Log.info("[green]WebLogger запущен на http://localhost:" + PORT + "[/]");
            Runtime.getRuntime().addShutdownHook(new Thread(WebLogger::stopServer));
        } catch (IOException e) {
            Log.err("Не удалось запустить WebLogger", e);
        }
    }

    public static void stopServer() {
        if (serverInstance != null) {
            serverInstance.stop();
            serverInstance = null;
            Log.info("[scarlet]WebLogger остановлен.[/]");
        }
    }
}