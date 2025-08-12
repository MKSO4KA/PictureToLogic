package com.mkso4ka.mindustry.matrixproc;

import com.mkso4ka.mindustry.matrixproc.Point;

public class DisplayProcessorMatrixFinal {
    final int n, m;
    final int[] processorsPerDisplay;
    final Point[] displays;
    final int displaySize;
    private final Cell[][] matrix;
    
    // --- ИСПРАВЛЕНИЕ: Возвращаем правильный радиус ---
    public static final int PROCESSOR_REACH = 10;

    public DisplayProcessorMatrixFinal(int n, int m, int[] processorsPerDisplay, Point[] displays, int displaySize) {
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
            Point p = displays[i];
            for (int row = p.y; row < p.y + displaySize; row++) {
                for (int col = p.x; col < p.x + displaySize; col++) {
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
            Point displayPos = displays[i];
            int placed = 0;
            for (int r = 1; r <= PROCESSOR_REACH && placed < processorsToPlace; r++) {
                for (int j = displayPos.x - r; j <= displayPos.x + displaySize - 1 + r && placed < processorsToPlace; j++) {
                    placed += tryPlaceProcessor(j, displayPos.y - r, i, placed, processorsToPlace);
                    placed += tryPlaceProcessor(j, displayPos.y + displaySize - 1 + r, i, placed, processorsToPlace);
                }
                for (int j = displayPos.y - r + 1; j <= displayPos.y + displaySize - 1 + r - 1 && placed < processorsToPlace; j++) {
                    placed += tryPlaceProcessor(displayPos.x - r, j, i, placed, processorsToPlace);
                    placed += tryPlaceProcessor(displayPos.x + displaySize - 1 + r, j, i, placed, processorsToPlace);
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
    public Point[] getDisplays() { return displays; }

    public static class Cell {
        public int type = 0; // 0 - empty, 1 - processor, 2 - display
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
            Point p = blueprint.displayBottomLefts[i];
            for (int row = p.y; row < p.y + displaySize; row++) {
                for (int col = p.x; col < p.x + displaySize; col++) {
                    if (row < blueprint.m && col < blueprint.n) {
                        matrix[row][col].type = 2;
                        matrix[row][col].ownerId = i;
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