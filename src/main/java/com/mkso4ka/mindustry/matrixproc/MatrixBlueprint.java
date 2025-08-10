package com.mkso4ka.mindustry.matrixproc;

class MatrixBlueprint {
    final int n;
    final int m;
    final int[][] displayCoordinates;

    MatrixBlueprint(int n, int m, int[][] coordinates) {
        this.n = n;
        this.m = m;
        this.displayCoordinates = coordinates;
    }
}