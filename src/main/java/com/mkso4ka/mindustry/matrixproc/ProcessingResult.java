package com.mkso4ka.mindustry.matrixproc;

import mindustry.game.Schematic;

/**
 * Класс-контейнер для передачи результатов обработки из LogicCore в UI.
 * Содержит как финальный чертеж, так и данные для отладки.
 */
public class ProcessingResult {
    public final Schematic schematic;
    public final DisplayProcessorMatrixFinal.Cell[][] matrix;
    public final DisplayInfo[] displays;
    public final int matrixWidth;
    public final int matrixHeight;
    public final int displaySize;

    public ProcessingResult(Schematic schematic, DisplayProcessorMatrixFinal.Cell[][] matrix, DisplayInfo[] displays, int displaySize) {
        this.schematic = schematic;
        this.matrix = matrix;
        this.displays = displays;
        this.displaySize = displaySize;
        this.matrixHeight = matrix.length;
        this.matrixWidth = matrix[0].length;
    }
}