package com.mkso4ka.mindustry.matrixproc;

// Используем DPoint из библиотеки триангуляции
import org.waveware.delaunator.DPoint;

public class DisplayMatrix {
    public MatrixBlueprint placeDisplaysXxY(int x, int y, int displaySize, int processorReach) {
        int spacing = processorReach * 2;
        int totalWidth = x * displaySize + (x - 1) * spacing;
        int totalHeight = y * displaySize + (y - 1) * spacing;

        // Создаем массив DPoint[]
        DPoint[] displayBottomLefts = new DPoint[x * y];
        int currentX = 0;
        for (int i = 0; i < x; i++) {
            int currentY = 0;
            for (int j = 0; j < y; j++) {
                // Создаем экземпляры DPoint
                displayBottomLefts[i * y + j] = new DPoint(currentX, currentY);
                currentY += displaySize + spacing;
            }
            currentX += displaySize + spacing;
        }
        return new MatrixBlueprint(totalWidth, totalHeight, displayBottomLefts);
    }
}
