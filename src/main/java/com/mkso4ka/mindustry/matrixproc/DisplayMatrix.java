package com.mkso4ka.mindustry.matrixproc;

// ИСПРАВЛЕНИЕ: Импортируем DPoint
import org.waveware.delaunator.DPoint;

class MatrixBlueprint {
    final int n, m; // Ширина и высота итоговой матрицы в тайлах
    final DPoint[] displayBottomLefts; // ИСПРАВЛЕНО: Тип изменен на DPoint[]

    MatrixBlueprint(int n, int m, DPoint[] displayBottomLefts) { // ИСПРАВЛЕНО
        this.n = n;
        this.m = m;
        this.displayBottomLefts = displayBottomLefts;
    }
}

public class DisplayMatrix {
    public MatrixBlueprint placeDisplaysXxY(int displaysX, int displaysY, int displaySize, int processorReach) {
        int gap = processorReach * 2;
        int totalWidth = displaysX * displaySize + (displaysX - 1) * gap;
        int totalHeight = displaysY * displaySize + (displaysY - 1) * gap;

        // ИСПРАВЛЕНО: Создаем массив DPoint[]
        DPoint[] displayCoords = new DPoint[displaysX * displaysY];
        for (int i = 0; i < displaysY; i++) {
            for (int j = 0; j < displaysX; j++) {
                int x = j * (displaySize + gap);
                int y = i * (displaySize + gap);
                displayCoords[j * displaysY + i] = new DPoint(x, y); // ИСПРАВЛЕНО
            }
        }
        return new MatrixBlueprint(totalWidth, totalHeight, displayCoords);
    }
}