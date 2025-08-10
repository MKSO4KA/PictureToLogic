package com.mkso4ka.mindustry.matrixproc;

import static java.lang.Math.ceil;
import static java.lang.Math.max;

/**
 * Вычисляет общие размеры схемы и координаты нижних левых углов для каждого дисплея.
 */
class DisplayMatrix {
    public DisplayMatrix() {}

    public MatrixBlueprint placeDisplaysXxY(int x, int y, int displaySize, double processorReach) {
        int border = (int) ceil(processorReach) + 1;
        int spacing = 0;

        // Размеры матрицы (n - высота, m - ширина)
        int n = border * 2 + y * displaySize + max(0, y - 1) * spacing;
        int m = border * 2 + x * displaySize + max(0, x - 1) * spacing;

        // Теперь это не центры, а нижние левые углы
        int[][] bottomLefts = new int[y * x][2];
        int startOffset = border;
        int count = 0;

        for (int j = 0; j < x; j++) { // Внешний цикл по X (колонки)
            for (int i = 0; i < y; i++) { // Внутренний цикл по Y (ряды)
                
                // Вычисляем и сохраняем координаты НИЖНЕГО ЛЕВОГО угла, а не центра.
                
                // X-координата нижнего левого угла
                int topLeftX = startOffset + j * displaySize;
                bottomLefts[count][0] = topLeftX;

                // Y-координата нижнего левого угла (с инверсией для Mindustry)
                int topLeftY_java = startOffset + i * displaySize;
                int bottomLeftY_java = topLeftY_java + displaySize - 1; // Y нижнего края в координатах Java
                bottomLefts[count][1] = (n - 1) - bottomLeftY_java; // Инвертируем в координаты Mindustry

                count++;
            }
        }
        return new MatrixBlueprint(n, m, bottomLefts);
    }
}