package com.mkso4ka.mindustry.matrixproc;

import mindustry.game.Schematic;
// ИСПРАВЛЕНИЕ: Импортируем DPoint
import org.waveware.delaunator.DPoint;

public class ProcessingResult {
    public final Schematic schematic;
    public final DisplayProcessorMatrixFinal.Cell[][] matrix;
    public final DPoint[] displays; // ИСПРАВЛЕНО: Тип изменен на DPoint[]
    public final int displaySize;

    public ProcessingResult(Schematic schematic, DisplayProcessorMatrixFinal.Cell[][] matrix, DPoint[] displays, int displaySize) { // ИСПРАВЛЕНО
        this.schematic = schematic;
        this.matrix = matrix;
        this.displays = displays;
        this.displaySize = displaySize;
    }
}