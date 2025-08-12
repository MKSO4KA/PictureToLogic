package com.mkso4ka.mindustry.matrixproc;

// Используем DPoint из библиотеки триангуляции
import org.waveware.delaunator.DPoint;

public class MatrixBlueprint {
    public final int n, m;
    // Поле должно быть типа DPoint[]
    public final DPoint[] displayBottomLefts;

    public MatrixBlueprint(int n, int m, DPoint[] displayBottomLefts) {
        this.n = n;
        this.m = m;
        this.displayBottomLefts = displayBottomLefts;
    }
}
