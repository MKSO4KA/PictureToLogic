package com.mkso4ka.mindustry.matrixproc;

/**
 * Хранит результат работы DisplayMatrix: размеры и координаты дисплеев.
 */
class MatrixBlueprint {
    final int n;
    final int m;
    // Возвращаем ваш тип int[][]
    final int[][] displayBottomLefts;

    MatrixBlueprint(int n, int m, int[][] coordinates) {
        this.n = n;
        this.m = m;
        this.displayBottomLefts = coordinates;
    }
}