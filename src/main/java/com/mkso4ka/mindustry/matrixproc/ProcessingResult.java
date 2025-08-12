package com.mkso4ka.mindustry.matrixproc;

import mindustry.game.Schematic;

public class ProcessingResult {
    public final Schematic schematic;
    public final DisplayProcessorMatrixFinal.Cell[][] matrix;
    // ИСПРАВЛЕНИЕ: Тип поля теперь DisplayInfo[]
    public final DisplayInfo[] displays;
    public final int displaySize;
    // ИСПРАВЛЕНИЕ: Добавляем недостающие поля
    public final int matrixWidth;
    public final int matrixHeight;

    // ИСПРАВЛЕНИЕ: Конструктор принимает DisplayInfo[]
    public ProcessingResult(Schematic schematic, DisplayProcessorMatrixFinal.Cell[][] matrix, DisplayInfo[] displays, int displaySize) {
        this.schematic = schematic;
        this.matrix = matrix;
        this.displays = displays;
        this.displaySize = displaySize;
        this.matrixHeight = matrix.length;
        this.matrixWidth = matrix[0].length;
    }
}
