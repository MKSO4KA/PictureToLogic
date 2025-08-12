package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2;

/**
 * Хранит информацию о конкретном дисплее в схеме.
 */
class DisplayInfo {
    final int id;
    // Храним нижний левый угол, а не центр
    final Point2 bottomLeft;
    final int totalProcessorsRequired;
    int processorsPlaced = 0;

    DisplayInfo(int id, Point2 bottomLeft, int required) {
        this.id = id;
        this.bottomLeft = bottomLeft;
        this.totalProcessorsRequired = required;
    }

    public int getProcessorsNeeded() {
        return totalProcessorsRequired - processorsPlaced;
    }
}
