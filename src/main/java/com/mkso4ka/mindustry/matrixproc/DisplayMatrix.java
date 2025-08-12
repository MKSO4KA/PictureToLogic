package com.mkso4ka.mindustry.matrixproc;

import org.waveware.delaunator.DPoint;

public class DisplayMatrix {
    public MatrixBlueprint placeDisplaysXxY(int displaysX, int displaysY, int displaySize, int processorReach) {
        // Зазор между дисплеями равен удвоенному радиусу досягаемости,
        // чтобы у каждого дисплея была своя зона для процессоров.
        int gap = processorReach * 2;
        
        // Вычисляем размер "контентной" части - все дисплеи и зазоры между ними.
        int contentWidth = displaysX * displaySize + Math.max(0, displaysX - 1) * gap;
        int contentHeight = displaysY * displaySize + Math.max(0, displaysY - 1) * gap;

        // Итоговый размер матрицы должен включать рамку для процессоров ВОКРУГ всей сетки.
        // Это и есть ключевое исправление.
        int totalWidth = contentWidth + processorReach * 2;
        int totalHeight = contentHeight + processorReach * 2;

        DPoint[] displayCoords = new DPoint[displaysX * displaysY];
        for (int i = 0; i < displaysY; i++) {
            for (int j = 0; j < displaysX; j++) {
                // Смещаем координаты каждого дисплея, чтобы учесть начальную рамку.
                int x = processorReach + j * (displaySize + gap);
                int y = processorReach + i * (displaySize + gap);
                displayCoords[j * displaysY + i] = new DPoint(x, y);
            }
        }
        return new MatrixBlueprint(totalWidth, totalHeight, displayCoords);
    }
}