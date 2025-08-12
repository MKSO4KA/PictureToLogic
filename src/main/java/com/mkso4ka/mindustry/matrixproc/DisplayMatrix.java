package com.mkso4ka.mindustry.matrixproc;

public class DisplayMatrix {
    public MatrixBlueprint placeDisplaysXxY(int x, int y, int displaySize, int processorReach) {
        int spacing = processorReach * 2;
        int totalWidth = x * displaySize + (x - 1) * spacing;
        int totalHeight = y * displaySize + (y - 1) * spacing;

        // --- ИСПРАВЛЕНИЕ: Используем правильный класс Point ---
        Point[] displayBottomLefts = new Point[x * y];
        int currentX = 0;
        for (int i = 0; i < x; i++) {
            int currentY = 0;
            for (int j = 0; j < y; j++) {
                // --- ИСПРАВЛЕНИЕ: Создаем экземпляры Point, а не DPoint ---
                displayBottomLefts[i * y + j] = new Point(currentX, currentY);
                currentY += displaySize + spacing;
            }
            currentX += displaySize + spacing;
        }
        return new MatrixBlueprint(totalWidth, totalHeight, displayBottomLefts);
    }
}