package com.mkso4ka.mindustry.matrixproc;

import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;

// ИСПРАВЛЕНИЕ: Реализуем Json.Serializable для полного контроля над выводом
public class DisplayCodeInfo implements Json.Serializable {
    public int displayId;
    public String fullCode;
    public int displaySizePixels;

    public DisplayCodeInfo() {}

    public DisplayCodeInfo(int id, String code, int size) {
        this.displayId = id;
        this.fullCode = code;
        this.displaySizePixels = size;
    }

    @Override
    public void write(Json json) {
        json.writeValue("displayId", displayId);
        json.writeValue("fullCode", fullCode);
        json.writeValue("displaySizePixels", displaySizePixels);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        // Чтение нам не нужно, но интерфейс требует реализации
    }
}
