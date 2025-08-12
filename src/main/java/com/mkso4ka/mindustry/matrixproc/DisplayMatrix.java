package com.mkso4ka.mindustry.matrixproc;

import org.waveware.delaunator.DPoint;

// Класс MatrixBlueprint был удален из этого файла.
public class DisplayMatrix {
    public MatrixBlueprint placeDisplaysXxY(int displaysX, int displaysY, int displaySize, int processorReach) {
        int gap = processorReach * 2;
        int totalWidth = displaysX * displaySize + (displaysX - 1) * gap;
        int totalHeight = displaysY * displaySize + (displaysY - 1) * gap;

        DPoint[] displayCoords = new DPoint[displaysX * displaysY];
        for (int i = 0; i < displaysY; i++) {
            for (int j = 0; j < displaysX; j++) {
                int x = j * (displaySize + gap);
                int y = i * (displaySize + gap);
                displayCoords[j * displaysY + i] = new DPoint(x, y);
            }
        }
        return new MatrixBlueprint(totalWidth, totalHeight, displayCoords);
    }
}