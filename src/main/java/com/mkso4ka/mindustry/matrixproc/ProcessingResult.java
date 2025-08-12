package com.mkso4ka.mindustry.matrixproc;

import mindustry.game.Schematic;
// Используем DPoint из библиотеки триангуляции
import org.waveware.delaunator.DPoint;

public class ProcessingResult {
    public final Schematic schematic;
    public final DisplayProcessorMatrixFinal.Cell[][] matrix;
    // Поле должно быть типа DPoint[]
    public final DPoint[] displays;
    public final int displaySize;

    public ProcessingResult(Schematic schematic, DisplayProcessorMatrixFinal.Cell[][] matrix, DPoint[] displays, int displaySize) {
        this.schematic = schematic;
        this.matrix = matrix;
        this.displays = displays;
        this.displaySize = displaySize;
    }
}
