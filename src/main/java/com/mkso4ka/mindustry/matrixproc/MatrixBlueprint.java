package com.mkso4ka.mindustry.matrixproc;

/**
 * Хранит результат работы DisplayMatrix: размеры и координаты дисплеев.
 */
class MatrixBlueprint {
    final int n;
    final int m;
    // Теперь это не просто координаты, а конкретно нижние левые углы
    final int[][] displayBottomLefts;

    MatrixBlueprint(int n, int m, int[][] coordinates) {
        this.n = n;
        this.m = m;
        this.displayBottomLefts = coordinates;
    }
}