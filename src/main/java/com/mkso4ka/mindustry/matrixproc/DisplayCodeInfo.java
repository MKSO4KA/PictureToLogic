package com.mkso4ka.mindustry.matrixproc;

// Простой контейнер для передачи полного кода дисплея в API
public class DisplayCodeInfo {
    public int displayId;
    public String fullCode;
    public int displaySizePixels;

    // Пустой конструктор нужен для корректной де/сериализации JSON библиотекой Arc
    public DisplayCodeInfo() {}

    public DisplayCodeInfo(int id, String code, int size) {
        this.displayId = id;
        this.fullCode = code;
        this.displaySizePixels = size;
    }
}
