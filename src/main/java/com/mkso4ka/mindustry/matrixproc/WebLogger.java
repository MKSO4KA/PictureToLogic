package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.scene.ui.Button;
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
    private static final List<String> processorCodeLogs = new ArrayList<>();
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
        
        if ("/".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getMainPageHtml());
        }
        if ("/debug".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getDebugPageHtml());
        }
        if ("/logs".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getLogsPageHtml());
        }
        if ("/processor-codes".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getProcessorCodesPageHtml());
        }

        if ("/api/logs-text".equals(uri)) {
            String logContent;
            synchronized (logs) { logContent = String.join("\n", logs); }
            return newFixedLengthResponse(Response.Status.OK, "text/plain", logContent);
        }
        if ("/download-logs".equals(uri)) {
            String logContent;
            synchronized (logs) { logContent = String.join("\n", logs); }
            Response res = newFixedLengthResponse(Response.Status.OK, "text/plain", logContent);
            res.addHeader("Content-Disposition", "attachment; filename=\"picturetologic.log\"");
            return res;
        }
        if ("/api/display-codes".equals(uri)) {
            Response res = newFixedLengthResponse(Response.Status.OK, "application/json", latestDisplayCodesJson);
            res.addHeader("Access-Control-Allow-Origin", "*");
            return res;
        }
        if (uri.startsWith("/debug/image/")) {
            String imageName = uri.substring("/debug/image/".length());
            byte[] imageData = debugImages.get(imageName);
            if (imageData != null) {
                return newFixedLengthResponse(Response.Status.OK, "image/png", new ByteArrayInputStream(imageData), imageData.length);
            }
        }
        if ("/debug/list".equals(uri)) {
            String json = "[\"" + String.join("\",\"", debugImages.keySet()) + "\"]";
            return newFixedLengthResponse(Response.Status.OK, "application/json", json);
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

    public static void logProcessorCode(String code) {
        if (ENABLE_WEB_LOGGER && serverInstance != null) {
            synchronized (processorCodeLogs) {
                processorCodeLogs.add(code);
            }
        }
    }
    
    public static void clearProcessorCodeLogs() {
        if (ENABLE_WEB_LOGGER && serverInstance != null) {
            synchronized (processorCodeLogs) {
                processorCodeLogs.clear();
            }
        }
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

    public static void logImage(String name, byte[] pngData) {
        if (!ENABLE_WEB_LOGGER || pngData == null) return;
        try 
        {
            debugImages.put(name + ".png", pngData); // Добавляем расширение для ясности
        } 
        catch (Exception e) 
        {
            err("Failed to log image byte array %s", e, name);
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
            slider.changed(() -> info("UI Event: Slider '%s' set to %f", name, slider.getValue()));
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

    private String getStyle() {
        return "<style>" +
            "body{font-family:sans-serif;background-color:#2c2c2c;color:#e0e0e0; margin:20px;} " +
            "a{color:#ffd06b; text-decoration:none;} h1,h2,h3{border-bottom:1px solid #555; padding-bottom:5px; margin-top:30px;} " +
            "button, .button{background-color:#4a4a4a; color:#e0e0e0; border:1px solid #666; padding:10px 15px; margin:5px; border-radius:5px; cursor:pointer; font-size:16px; display:inline-block;}" +
            "button:hover, .button:hover{background-color:#5a5a5a;}" +
            ".container{display:flex; flex-wrap:wrap; gap: 20px; margin-top:20px;} " +
            ".slice{border:1px solid #555; padding:10px; background-color:#3c3c3c; border-radius:5px;} " +
            "img{border:1px solid #888; margin-top:5px; max-width:400px; image-rendering:pixelated; display:block;}" +
            "pre{white-space:pre-wrap;word-break:break-all; background-color:#1e1e1e; padding:10px; border-radius:5px;}" +
            "</style>";
    }

    private String getMainPageHtml() {
        return "<html><head><title>PictureToLogic Debug Hub</title>" + getStyle() + "</head>" +
            "<body><h1>PictureToLogic Debug Hub</h1>" +
            "<a href='/debug' class='button'>Visual Debugger (Slices)</a>" +
            "<a href='/logs' class='button'>Text Logs</a>" +
            "<a href='/processor-codes' class='button'>Processor Codes</a>" +
            "</body></html>";
    }

    private String getDebugPageHtml() {
        return "<html><head><title>Visual Debugger</title>" + getStyle() + "</head>" +
            "<body><h1>Visual Debugger</h1>" +
            "<a href='/' class='button'>Back to Hub</a>" +
            "<button onclick='location.reload()'>Refresh Images</button>" +
            "<div id='slicesContainer' class='container'></div>" +
            "<script>" +
            "const slicesDiv = document.getElementById('slicesContainer');" +
            "fetch('/debug/list').then(r => r.json()).then(files => {" +
            "  const slices = {};" +
            "  files.forEach(f => { if(f.startsWith('slice_')) { const parts = f.split('_'); const id = parts[1]; if(!slices[id]) slices[id] = []; slices[id].push(f); } });" +
            "  for(const id in slices){" +
            "    const sliceDiv = document.createElement('div'); sliceDiv.className = 'slice';" +
            "    sliceDiv.innerHTML = `<h2>Slice #${id}</h2>`;" +
            "    slices[id].sort().forEach(f => {" +
            "      const name = f.split('_').slice(2).join('_').replace('.png','');" +
            "      sliceDiv.innerHTML += `<div><h3>${name}</h3><img src='/debug/image/${f}?t=" + System.currentTimeMillis() + "'></div>`;" +
            "    });" +
            "    slicesDiv.appendChild(sliceDiv);" +
            "  }" +
            "});" +
            "</script></body></html>";
    }

    private String getLogsPageHtml() {
        return "<html><head><title>Text Logs</title>" + getStyle() + "</head>" +
            "<body><h1>Text Logs</h1>" +
            "<a href='/' class='button'>Back to Hub</a>" +
            "<a href='/download-logs' class='button'>Download as .log file</a>" +
            "<pre id='logs'>Loading...</pre>" +
            "<script>" +
            "const logsPre = document.getElementById('logs');" +
            "function fetchLogs(){ fetch('/api/logs-text').then(res => res.text()).then(text => { logsPre.textContent = text; }); }" +
            "setInterval(fetchLogs, 2000);" +
            "fetchLogs();" +
            "</script></body></html>";
    }

    private String getProcessorCodesPageHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>Processor Codes</title>").append(getStyle()).append("</head>");
        sb.append("<body><h1>Processor Codes</h1><a href='/' class='button'>Back to Hub</a>");
        synchronized (processorCodeLogs) {
            for (String codeBlock : processorCodeLogs) {
                sb.append("<pre>").append(codeBlock.replace("<", "&lt;").replace(">", "&gt;")).append("</pre>");
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
