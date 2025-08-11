package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.scene.ui.Button;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Slider;
import arc.util.Log;
import arc.util.serialization.Json;
import arc.util.serialization.JsonWriter;
import fi.iki.elonen.NanoHTTPD;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebLogger extends NanoHTTPD {

    public static final boolean ENABLE_WEB_LOGGER = true;

    private static final int PORT = 8080;
    private static final List<String> logs = new ArrayList<>();
    private static WebLogger serverInstance;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private static String latestDisplayCodesJson = "[]";
    private static final Map<String, byte[]> debugImages = new ConcurrentHashMap<>();

    private WebLogger() throws IOException {
        super(PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        
        if ("/api/display-codes".equals(uri)) {
            Response res = newFixedLengthResponse(Response.Status.OK, "application/json", latestDisplayCodesJson);
            res.addHeader("Access-Control-Allow-Origin", "*"); // Разрешаем кросс-доменные запросы
            return res;
        }

        if ("/logs".equals(uri)) {
            String response;
            synchronized (logs) { response = String.join("\n", logs); }
            return newFixedLengthResponse(Response.Status.OK, "text/plain", response);
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    public static void logDisplayCodes(List<DisplayCodeInfo> codes) {
        if (!ENABLE_WEB_LOGGER) return;
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        latestDisplayCodesJson = json.toJson(codes);
        info("Display codes updated for external API. %d displays.", codes.size());
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

    public static void logImage(String name, Pixmap pixmap) {
        if (!ENABLE_WEB_LOGGER || pixmap == null) return;
        Fi tempFile = Fi.tempFile("picturetologic-debug");
        try {
            PixmapIO.writePng(tempFile, pixmap);
            byte[] bytes = tempFile.readBytes();
            debugImages.put(name, bytes);
        } catch (Exception e) {
            err("Failed to log image %s", e);
        } finally {
            tempFile.delete();
        }
    }

    public static void clearDebugImages() {
        if (ENABLE_WEB_LOGGER) {
            debugImages.clear();
        }
    }

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
    
    public static <T extends Button> T logClick(T button, String name) {
        if (ENABLE_WEB_LOGGER) {
            button.clicked(() -> info("UI Event: Clicked '%s'", name));
        }
        return button;
    }

    public static Slider logChange(Slider slider, String name) {
        if (ENABLE_WEB_LOGGER) {
            slider.changed(() -> info("UI Event: Slider '%s' set to %d", name, (int)slider.getValue()));
        }
        return slider;
    }

    public static void logShow(BaseDialog dialog, String name) {
        if (ENABLE_WEB_LOGGER) {
            info("UI Event: Dialog '%s' shown", name);
        }
        dialog.show();
    }

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
}
