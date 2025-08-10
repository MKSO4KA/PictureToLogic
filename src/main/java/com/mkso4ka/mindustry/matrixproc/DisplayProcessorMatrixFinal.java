package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2;
import arc.util.Log;
import java.util.LinkedList;
import java.util.Queue;

class DisplayProcessorMatrixFinal {

    // Вспомогательный класс для хранения кандидата в очереди
    private static class ProcessorCandidate {
        final Point2 location;
        final int ownerId;

        ProcessorCandidate(Point2 location, int ownerId) {
            this.location = location;
            this.ownerId = ownerId;
        }
    }

    static class Cell {
        int type = 0;
        int ownerId = -1;
        // НОВОЕ ПОЛЕ: Хранит порядковый номер процессора для его владельца (0, 1, 2...)
        int processorIndex = -1;
    }

    public static final double PROCESSOR_REACH = 10.2;
    private static final double PROCESSOR_REACH_SQ = PROCESSOR_REACH * PROCESSOR_REACH;

    private final int n; // высота (y)
    private final int m; // ширина (x)
    private final Cell[][] matrix;
    private final DisplayInfo[] displays;
    private final int displaySize;

    public DisplayProcessorMatrixFinal(int n, int m, int[] processorsPerDisplay, int[][] displayCenters, int displaySize) {
        this.n = n;
        this.m = m;
        this.displaySize = displaySize;
        this.matrix = new Cell[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                matrix[i][j] = new Cell();
            }
        }
        this.displays = new DisplayInfo[displayCenters.length];
        for (int i = 0; i < displayCenters.length; i++) {
            Point2 center = new Point2(displayCenters[i][0], displayCenters[i][1]);
            displays[i] = new DisplayInfo(i, center, processorsPerDisplay[i]);
            placeSingleDisplay(displays[i], displaySize);
        }
    }

    public Cell[][] getMatrix() { return this.matrix; }
    public DisplayInfo[] getDisplays() { return this.displays; }

    private void placeSingleDisplay(DisplayInfo display, int displaySize) {
        int offset = displaySize / 2;
        int start = -offset;
        int end = (displaySize % 2 == 0) ? offset - 1 : offset;
        for (int i = start; i <= end; i++) {
            for (int j = start; j <= end; j++) {
                int currentX = display.center.x + j;
                int currentY = display.center.y + i;
                if (currentX >= 0 && currentX < m && currentY >= 0 && currentY < n) {
                    matrix[currentY][currentX].type = 2;
                    matrix[currentY][currentX].ownerId = display.id;
                }
            }
        }
    }

    public void placeProcessors() {
        // --- ЭТАП 1: Определяем всю разрешенную территорию для процессоров ---
        boolean[][] isAllowed = new boolean[n][m];
        boolean[][] visitedForTerritory = new boolean[n][m];
        Queue<Point2> territoryQueue = new LinkedList<>();

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                if (matrix[y][x].type == 2) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < m && ny >= 0 && ny < n && matrix[ny][nx].type == 0 && !visitedForTerritory[ny][nx] && isWithinProcessorReachOfAnyDisplay(new Point2(nx, ny))) {
                                territoryQueue.add(new Point2(nx, ny));
                                visitedForTerritory[ny][nx] = true;
                            }
                        }
                    }
                }
            }
        }

        while (!territoryQueue.isEmpty()) {
            Point2 current = territoryQueue.poll();
            isAllowed[current.y][current.x] = true;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = current.x + dx;
                    int ny = current.y + dy;
                    if (nx >= 0 && nx < m && ny >= 0 && ny < n && matrix[ny][nx].type == 0 && !visitedForTerritory[ny][nx] && isWithinProcessorReachOfAnyDisplay(new Point2(nx, ny))) {
                        territoryQueue.add(new Point2(nx, ny));
                        visitedForTerritory[ny][nx] = true;
                    }
                }
            }
        }

        // --- ЭТАП 2: Параллельная заливка (BFS) от каждого дисплея для компактного размещения ---
        Queue<ProcessorCandidate> placementQueue = new LinkedList<>();
        boolean[][] visitedForPlacement = new boolean[n][m];

        for (DisplayInfo display : displays) {
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < m; x++) {
                    if (matrix[y][x].type == 2 && matrix[y][x].ownerId == display.id) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                if (dx == 0 && dy == 0) continue;
                                int nx = x + dx;
                                int ny = y + dy;
                                if (nx >= 0 && nx < m && ny >= 0 && ny < n && isAllowed[ny][nx] && !visitedForPlacement[ny][nx]) {
                                    if (distanceSqFromPointToRectangle(new Point2(nx, ny), display) <= PROCESSOR_REACH_SQ) {
                                        placementQueue.add(new ProcessorCandidate(new Point2(nx, ny), display.id));
                                        visitedForPlacement[ny][nx] = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        while (!placementQueue.isEmpty()) {
            ProcessorCandidate candidate = placementQueue.poll();
            DisplayInfo owner = displays[candidate.ownerId];

            if (owner.getProcessorsNeeded() > 0) {
                // --- ГЛАВНОЕ ИСПРАВЛЕНИЕ ---
                // Присваиваем процессору его порядковый номер, перед тем как увеличить счетчик.
                matrix[candidate.location.y][candidate.location.x].processorIndex = owner.processorsPlaced;
                
                matrix[candidate.location.y][candidate.location.x].type = 1;
                matrix[candidate.location.y][candidate.location.x].ownerId = owner.id;
                owner.processorsPlaced++;

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = candidate.location.x + dx;
                        int ny = candidate.location.y + dy;
                        if (nx >= 0 && nx < m && ny >= 0 && ny < n && isAllowed[ny][nx] && !visitedForPlacement[ny][nx]) {
                            if (distanceSqFromPointToRectangle(new Point2(nx, ny), owner) <= PROCESSOR_REACH_SQ) {
                                placementQueue.add(new ProcessorCandidate(new Point2(nx, ny), owner.id));
                                visitedForPlacement[ny][nx] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isWithinProcessorReachOfAnyDisplay(Point2 p) {
        for (DisplayInfo display : displays) {
            if (distanceSqFromPointToRectangle(p, display) <= PROCESSOR_REACH_SQ) {
                return true;
            }
        }
        return false;
    }

    private double distanceSqFromPointToRectangle(Point2 p, DisplayInfo display) {
        int halfSize = displaySize / 2;
        int minX = display.center.x - halfSize;
        int maxX = display.center.x + ((displaySize % 2 == 0) ? halfSize - 1 : halfSize);
        int minY = display.center.y - halfSize;
        int maxY = display.center.y + ((displaySize % 2 == 0) ? halfSize - 1 : halfSize);
        
        double closestX = Math.max(minX, Math.min(p.x, maxX));
        double closestY = Math.max(minY, Math.min(p.y, maxY));
        
        double dx = p.x - closestX;
        double dy = p.y - closestY;
        
        return dx * dx + dy * dy;
    }
}