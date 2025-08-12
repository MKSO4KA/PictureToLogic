package com.mkso4ka.mindustry.matrixproc;

import org.waveware.delaunator.DPoint;

public class DisplayMatrix {
    public MatrixBlueprint placeDisplaysXxY(int x, int y, int displaySize, int processorReach) {
        // Расстояние между краями соседних дисплеев
        int spacing = processorReach * 2;
        
        // --- ИСПРАВЛЕНИЕ: Правильный расчет общего размера сетки ---
        
        // Сначала считаем размер "контентной" части (только дисплеи и промежутки)
        int contentWidth = x * displaySize + Math.max(0, x - 1) * spacing;
        int contentHeight = y * displaySize + Math.max(0, y - 1) * spacing;
        
        // Затем добавляем поля (PROCESSOR_REACH) со всех сторон
        int totalGridWidth = contentWidth + 2 * processorReach;
        int totalGridHeight = contentHeight + 2 * processorReach;

        DPoint[] displayBottomLefts = new DPoint[x * y];
        
        // --- ИСПРАВЛЕНИЕ: Начинаем размещение с отступом, равным радиусу ---
        int currentX = processorReach; 
        for (int i = 0; i < x; i++) {
            int currentY = processorReach; // Сбрасываем Y для каждого нового столбца
            for (int j = 0; j < y; j++) {
                displayBottomLefts[i * y + j] = new DPoint(currentX, currentY);
                currentY += displaySize + spacing;
            }
            currentX += displaySize + spacing;
        }
        
        return new MatrixBlueprint(totalGridWidth, totalGridHeight, displayBottomLefts);
    }
}