package com.mkso4ka.mindustry.matrixproc;

import org.waveware.delaunator.DPoint;

public class MatrixBlueprint {
    final int n, m; // Ширина (n) и высота (m) итоговой матрицы в тайлах
    final DPoint[] displayBottomLefts;

    MatrixBlueprint(int n, int m, DPoint[] displayBottomLefts) {
        this.n = n;
        this.m = m;
        this.displayBottomLefts = displayBottomLefts;
    }
}