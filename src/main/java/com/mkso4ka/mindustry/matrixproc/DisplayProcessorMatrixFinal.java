package com.mkso4ka.mindustry.matrixproc;

import org.waveware.delaunator.DPoint;

public class DisplayProcessorMatrixFinal {
    final int n, m;
    final int[] processorsPerDisplay;
    final DPoint[] displays;
    final int displaySize;
    private final Cell[][] matrix;
    
    public static final int PROCESSOR_REACH = 10;

    public DisplayProcessorMatrixFinal(int n, int m, int[] processorsPerDisplay, DPoint[] displays, int displaySize) {
        this.n = n;
        this.m = m;
        this.processorsPerDisplay = processorsPerDisplay;
        this.displays = displays;
        this.displaySize = displaySize;
        this.matrix = new Cell[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = new Cell();
            }
        }
        for (int i = 0; i < displays.length; i++) {
            DPoint p = displays[i];
            for (int row = (int)p.y; row < (int)p.y + displaySize; row++) {
                for (int col = (int)p.x; col < (int)p.x + displaySize; col++) {
                    if (row < m && col < n) {
                        matrix[row][col].type = 2;
                        matrix[row][col].ownerId = i;
                    }
                }
            }
        }
    }

    public void placeProcessors() {
        for (int i = 0; i < displays.length; i++) {
            int processorsToPlace = processorsPerDisplay[i];
            DPoint displayPos = displays[i];
            int placed = 0;
            int dispX = (int)displayPos.x;
            int dispY = (int)displayPos.y;
            for (int r = 1; r <= PROCESSOR_REACH && placed < processorsToPlace; r++) {
                for (int j = dispX - r; j <= dispX + displaySize - 1 + r && placed < processorsToPlace; j++) {
                    placed += tryPlaceProcessor(j, dispY - r, i, placed, processorsToPlace);
                    placed += tryPlaceProcessor(j, dispY + displaySize - 1 + r, i, placed, processorsToPlace);
                }
                for (int j = dispY - r + 1; j <= dispY + displaySize - 1 + r - 1 && placed < processorsToPlace; j++) {
                    placed += tryPlaceProcessor(dispX - r, j, i, placed, processorsToPlace);
                    placed += tryPlaceProcessor(dispX + displaySize - 1 + r, j, i, placed, processorsToPlace);
                }
            }
        }
    }

    private int tryPlaceProcessor(int x, int y, int ownerId, int placed, int toPlace) {
        if (placed < toPlace && y >= 0 && y < m && x >= 0 && x < n && matrix[y][x].type == 0) {
            matrix[y][x].type = 1;
            matrix[y][x].ownerId = ownerId;
            matrix[y][x].processorIndex = placed;
            return 1;
        }
        return 0;
    }

    public Cell[][] getMatrix() { return matrix; }
    public DPoint[] getDisplays() { return displays; }

    public static class Cell {
        public int type = 0;
        public int ownerId = -1;
        public int processorIndex = -1;
    }

    public static int calculateMaxAvailableProcessors(int n, int m, int displaySize) {
        DisplayMatrix displayMatrix = new DisplayMatrix();
        MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(n, m, displaySize, PROCESSOR_REACH);

        Cell[][] matrix = new Cell[blueprint.m][blueprint.n];
        for (int i = 0; i < blueprint.m; i++) {
            for (int j = 0; j < blueprint.n; j++) {
                matrix[i][j] = new Cell();
            }
        }

        for (int i = 0; i < blueprint.displayBottomLefts.length; i++) {
            DPoint p = blueprint.displayBottomLefts[i];
            for (int row = (int)p.y; row < (int)p.y + displaySize; row++) {
                for (int col = (int)p.x; col < (int)p.x + displaySize; col++) {
                    if (row < blueprint.m && col < blueprint.n) {
                        matrix[row][col].type = 2;
                    }
                }
            }
        }

        int availableSlots = 0;
        for (int i = 0; i < blueprint.m; i++) {
            for (int j = 0; j < blueprint.n; j++) {
                if (matrix[i][j].type == 0) {
                    availableSlots++;
                }
            }
        }
        return availableSlots;
    }
}