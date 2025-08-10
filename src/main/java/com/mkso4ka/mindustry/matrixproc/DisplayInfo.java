package com.mkso4ka.mindustry.matrixproc;

import java.awt.Point;

class DisplayInfo {
    final int id;
    final Point center;
    final int totalProcessorsRequired;
    int processorsPlaced = 0;

    DisplayInfo(int id, Point center, int required) {
        this.id = id;
        this.center = center;
        this.totalProcessorsRequired = required;
    }

    public int getProcessorsNeeded() {
        return totalProcessorsRequired - processorsPlaced;
    }

    double distanceSq(Point p) {
        double dx = p.x - center.x;
        double dy = p.y - center.y;
        return dx * dx + dy * dy;
    }
}