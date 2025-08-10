package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2; // ИЗМЕНЕНИЕ: Используем Point2 из Arc

class DisplayInfo {
    final int id;
    final Point2 center; // ИЗМЕНЕНИЕ: Тип поля изменен
    final int totalProcessorsRequired;
    int processorsPlaced = 0;

    DisplayInfo(int id, Point2 center, int required) { // ИЗМЕНЕНИЕ: Тип параметра изменен
        this.id = id;
        this.center = center;
        this.totalProcessorsRequired = required;
    }

    public int getProcessorsNeeded() {
        return totalProcessorsRequired - processorsPlaced;
    }

    double distanceSq(Point2 p) { // ИЗМЕНЕНИЕ: Тип параметра изменен
        // Point2 использует float, но для квадрата расстояния это не имеет значения
        double dx = p.x - center.x;
        double dy = p.y - center.y;
        return dx * dx + dy * dy;
    }
}