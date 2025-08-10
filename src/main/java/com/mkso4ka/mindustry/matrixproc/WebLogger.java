package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.func.Cons;
import arc.scene.ui.Button;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Slider;
import arc.util.Log;
import fi.iki.elonen.NanoHTTPD;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Кастомный логгер, который запускает локальный веб-сервер для отображения логов в реальном времени.
 * Также предоставляет обертки для UI-элементов для автоматического логирования действий.
 */
public class WebLogger extends NanoHTTPD {

    /**
     * ГЛАВНАЯ КОНСТАНТА: Установите false, чтобы полностью отключить веб-сервер и логгирование через него.
     * При значении false компилятор вырежет весь связанный код.
     */
    public static final boolean ENABLE_WEB_LOGGER = true;

    private static final int PORT = 8080;
    private static final List<String> logs = new ArrayList<>();
    private static WebLogger serverInstance;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private WebLogger() throws IOException {
        super(PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

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

    public static void info(String text, Object... args) { log("I", text, args); }
    public static void warn(String text, Object... args) { log("W", text, args); }
    public static void err(String text, Object... args) { log("E", text, args); }

    // --- МЕТОДЫ-ОБЕРТКИ ДЛЯ UI ---

    /** Обертка для кнопок. Логирует нажатие. */
    public static <T extends Button> T logClick(T button, String name) {
        if (ENABLE_WEB_LOGGER) {
            button.clicked(() -> info("UI Event: Clicked '%s'", name));
        }
        return button;
    }

    /** Обертка для слайдеров. Логирует изменение значения. */
    public static Slider logChange(Slider slider, String name) {
        if (ENABLE_WEB_LOGGER) {
            slider.changed(() -> info("UI Event: Slider '%s' set to %d", name, (int)slider.getValue()));
        }
        return slider;
    }

    /** Обертка для галочек (CheckBox). Логирует изменение состояния. */
    public static CheckBox logToggle(CheckBox checkBox, String name) {
        if (ENABLE_WEB_LOGGER) {
            checkBox.changed(() -> info("UI Event: CheckBox '%s' is now %s", name, checkBox.isChecked() ? "checked" : "unchecked"));
        }
        return checkBox;
    }

    /** Обертка для показа диалога. */
    public static void logShow(BaseDialog dialog, String name) {
        if (ENABLE_WEB_LOGGER) {
            info("UI Event: Dialog '%s' shown", name);
        }
        dialog.show();
    }

    /** Обертка для выбора файла. */
    public static void logFileChooser(Cons<Fi> callback) {
        if (ENABLE_WEB_LOGGER) {
            info("UI Event: File chooser opened");
        }
        Vars.platform.showFileChooser(true, "Выбор изображения", "png", file -> {
            if (ENABLE_WEB_LOGGER) {
                if (file != null) {
                    info("UI Event: File selected: %s", file.name());
                } else {
                    info("UI Event: File selection cancelled");
                }
            }
            callback.get(file);
        });
    }

    // --- Управление сервером ---

    public static void startServer() {
        if (!ENABLE_WEB_LOGGER || serverInstance != null) return;
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