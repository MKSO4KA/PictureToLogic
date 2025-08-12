package com.mkso4ka.mindustry.matrixproc;

// Используем DPoint из библиотеки триангуляции
import org.waveware.delaunator.DPoint;

public class DisplayInfo {
    // Поле должно быть типа DPoint
    public final DPoint bottomLeft;
    public final int id;

    public DisplayInfo(DPoint bottomLeft, int id) {
        this.bottomLeft = bottomLeft;
        this.id = id;
    }
}
