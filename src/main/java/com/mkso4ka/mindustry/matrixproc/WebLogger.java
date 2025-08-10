package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.func.Cons;
import arc.scene.ui.Button;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Slider;
import arc.util.Log;
import arc.util.serialization.Json;
import com.mkso4ka.mindustry.matrixproc.debug.SchematicData;
import fi.iki.elonen.NanoHTTPD;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

import java.io.ByteArrayOutputStream;
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

    private static final Map<String, byte[]> debugImages = new ConcurrentHashMap<>();
    private static String latestSchematicJson = "{}";

    private WebLogger() throws IOException {
        super(PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if ("/".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getMainPageHtml());
        }
        if ("/debug".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getDebugPageHtml());
        }
        if ("/logs".equals(uri)) {
            if (Method.GET.equals(method)) {
                String response;
                synchronized (logs) { response = String.join("\n", logs); }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", response);
            } else if (Method.DELETE.equals(method)) {
                synchronized (logs) { logs.clear(); }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Logs cleared.");
            }
        }
        if (uri.startsWith("/debug/image/")) {
            String imageName = uri.substring("/debug/image/".length());
            byte[] imageData = debugImages.get(imageName);
            if (imageData != null) {
                return newFixedLengthResponse(Response.Status.OK, "image/png", imageData, imageData.length);
            }
        }
        if ("/debug/list".equals(uri)) {
            String json = "[\"" + String.join("\",\"", debugImages.keySet()) + "\"]";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
        }
        if ("/api/schematic-data".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", latestSchematicJson);
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

    public static void logImage(String name, arc.graphics.Pixmap pixmap) {
        if (!ENABLE_WEB_LOGGER || pixmap == null) return;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            arc.graphics.g2d.PixmapIO.writePng(pixmap, baos);
            debugImages.put(name, baos.toByteArray());
            info("Logged debug image: %s", name);
        } catch (IOException e) {
            err("Failed to log image %s", name);
        }
    }

    public static void clearDebugImages() {
        if (ENABLE_WEB_LOGGER) {
            debugImages.clear();
        }
    }

    public static void logSchematicData(SchematicData data) {
        if (!ENABLE_WEB_LOGGER) return;
        Json json = new Json();
        latestSchematicJson = json.toJson(data);
        info("Schematic data updated for external debugger.");
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

    public static CheckBox logToggle(CheckBox checkBox, String name) {
        if (ENABLE_WEB_LOGGER) {
            checkBox.changed(() -> info("UI Event: CheckBox '%s' is now %s", name, checkBox.isChecked() ? "checked" : "unchecked"));
        }
        return checkBox;
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

    private String getMainPageHtml() {
        return "<html><head><title>PictureToLogic Logs</title>" +
            "<style>body{font-family:monospace;background-color:#2c2c2c;color:#e0e0e0;} a{color:#ffd06b;} pre{white-space:pre-wrap;word-break:break-all;} button{padding:10px;}</style></head>" +
            "<body><h1>PictureToLogic Live Logs</h1><p><a href='/debug'>Go to Visual Debugger</a></p><button onclick='clearLogs()'>Clear Logs</button><pre id='logs'></pre>" +
            "<script>" +
            "const logsPre = document.getElementById('logs');" +
            "function fetchLogs(){ fetch('/logs').then(res => res.text()).then(text => { logsPre.textContent = text; logsPre.scrollTop = logsPre.scrollHeight; }); }" +
            "function clearLogs(){ fetch('/logs', { method: 'DELETE' }).then(() => fetchLogs()); }" +
            "setInterval(fetchLogs, 2000);" +
            "fetchLogs();" +
            "</script></body></html>";
    }

    private String getDebugPageHtml() {
        return "<html><head><title>Visual Debugger</title>" +
            "<style>body{font-family:sans-serif;background-color:#2c2c2c;color:#e0e0e0;} a{color:#ffd06b;} .slice{border:1px solid #555; margin:10px; padding:10px; display:inline-block;} img{border:1px solid #888; margin:5px;}</style></head>" +
            "<body><h1>Visual Debugger</h1><p><a href='/'>Back to Text Logs</a></p><div id='slices'></div>" +
            "<script>" +
            "const slicesDiv = document.getElementById('slices');" +
            "fetch('/debug/list').then(r => r.json()).then(files => {" +
            "  const slices = {};" +
            "  files.forEach(f => { const parts = f.split('_'); const id = parts[1]; if(!slices[id]) slices[id] = []; slices[id].push(f); });" +
            "  for(const id in slices){" +
            "    const sliceDiv = document.createElement('div'); sliceDiv.className = 'slice';" +
            "    sliceDiv.innerHTML = `<h2>Slice #${id}</h2>`;" +
            "    slices[id].sort().forEach(f => {" +
            "      const name = f.split('_').slice(2).join('_');" +
            "      sliceDiv.innerHTML += `<div><h3>${name}</h3><img src='/debug/image/${f}'></div>`;" +
            "    });" +
            "    slicesDiv.appendChild(sliceDiv);" +
            "  }" +
            "});" +
            "</script></body></html>";
    }
}