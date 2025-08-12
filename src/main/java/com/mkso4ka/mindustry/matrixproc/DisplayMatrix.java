package com.mkso4ka.mindustry.matrixproc;

import org.waveware.delaunator.DPoint;

public class DisplayMatrix {
    public MatrixBlueprint placeDisplaysXxY(int displaysX, int displaysY, int displaySize, int processorReach) {
        int gap = processorReach * 2;
        
        int contentWidth = displaysX * displaySize + Math.max(0, displaysX - 1) * gap;
        int contentHeight = displaysY * displaySize + Math.max(0, displaysY - 1) * gap;

        int totalWidth = contentWidth + processorReach * 2;
        int totalHeight = contentHeight + processorReach * 2;

        DPoint[] displayCoords = new DPoint[displaysX * displaysY];
        for (int i = 0; i < displaysY; i++) {
            for (int j = 0; j < displaysX; j++) {
                int x = processorReach + j * (displaySize + gap);
                int y = processorReach + i * (displaySize + gap);
                displayCoords[j * displaysY + i] = new DPoint(x, y);
            }
        }
        return new MatrixBlueprint(totalWidth, totalHeight, displayCoords);
    }
}
