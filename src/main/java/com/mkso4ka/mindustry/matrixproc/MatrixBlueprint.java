package com.mkso4ka.mindustry.matrixproc;

import org.waveware.delaunator.DPoint;

// Этот класс должен быть public и находиться в своем собственном файле.
public class MatrixBlueprint {
    final int n, m; // Ширина и высота итоговой матрицы в тайлах
    final DPoint[] displayBottomLefts;

    MatrixBlueprint(int n, int m, DPoint[] displayBottomLefts) {
        this.n = n;
        this.m = m;
        this.displayBottomLefts = displayBottomLefts;
    }
}