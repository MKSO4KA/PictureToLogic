package com.mkso4ka.mindustry.matrixproc;

class DisplayMatrix {
    public DisplayMatrix() {}

    public MatrixBlueprint placeDisplaysXxY(int x, int y, int displaySize, double processorReach) {
        int border = (int) Math.ceil(processorReach) + 1;
        int spacing = 0;

        int n = border * 2 + y * displaySize + Math.max(0, y - 1) * spacing;
        int m = border * 2 + x * displaySize + Math.max(0, x - 1) * spacing;

        int[][] centers = new int[y * x][2];
        int startOffset = border;
        int count = 0;

        // =================================================================
        // КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ 1: Порядок итерации
        // Теперь итерация идет сначала по колонкам (j), потом по рядам (i),
        // чтобы соответствовать порядку в Main.java.
        // =================================================================
        for (int j = 0; j < x; j++) { // Внешний цикл по X (колонки)
            for (int i = 0; i < y; i++) { // Внутренний цикл по Y (ряды)
                
                // Расчет X-координаты центра (остается без изменений)
                int topLeftX = startOffset + j * displaySize;
                centers[count][0] = topLeftX + displaySize / 2;

                // =================================================================
                // КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ 2: Инверсия оси Y
                // Мы рассчитываем Y-координату как в Java (от верхнего края),
                // а затем "переворачиваем" ее для системы координат Mindustry.
                // =================================================================
                int topLeftY_java = startOffset + i * displaySize;
                int centerY_java = topLeftY_java + displaySize / 2;
                centers[count][1] = (n - 1) - centerY_java; // (n-1) - это последняя Y-координата в матрице

                count++;
            }
        }
        return new MatrixBlueprint(n, m, centers);
    }
}