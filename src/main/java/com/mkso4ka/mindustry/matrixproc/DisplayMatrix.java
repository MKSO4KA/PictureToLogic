package com.mkso4ka.mindustry.matrixproc;

import static java.lang.Math.ceil;
import static java.lang.Math.max;

// Этот класс полностью основан на вашей логике
class DisplayMatrix {
    public DisplayMatrix() {}

    public MatrixBlueprint placeDisplaysXxY(int x, int y, int displaySize, double processorReach) {
        int border = (int) ceil(processorReach) + 1;
        int spacing = 0;

        // Размеры матрицы (n - высота, m - ширина)
        int n = border * 2 + y * displaySize + max(0, y - 1) * spacing;
        int m = border * 2 + x * displaySize + max(0, x - 1) * spacing;

        int[][] centers = new int[y * x][2];
        int startOffset = border;
        int count = 0;

        // ВАША логика итерации и инверсии оси Y, которая работает правильно
        for (int j = 0; j < x; j++) { // Внешний цикл по X (колонки)
            for (int i = 0; i < y; i++) { // Внутренний цикл по Y (ряды)
                
                // Расчет X-координаты центра
                int topLeftX = startOffset + j * (displaySize + spacing);
                centers[count][0] = topLeftX + displaySize / 2;

                // Расчет Y-координаты центра с инверсией
                int topLeftY_java = startOffset + i * (displaySize + spacing);
                int centerY_java = topLeftY_java + displaySize / 2;
                centers[count][1] = (n - 1) - centerY_java;

                count++;
            }
        }
        // Возвращаем blueprint с правильными размерами и координатами
        return new MatrixBlueprint(n, m, centers);
    }
}