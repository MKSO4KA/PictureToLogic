package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2;
import arc.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

class DisplayProcessorMatrixFinal {

    static class Cell {
        int type = 0;
        int ownerId = -1;
    }

    public static final double PROCESSOR_REACH = 10.2;
    private static final double PROCESSOR_REACH_SQ = PROCESSOR_REACH * PROCESSOR_REACH;

    private final int n;
    private final int m;
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

    public Cell[][] getMatrix() {
        return this.matrix;
    }
    
    public DisplayInfo[] getDisplays() {
        return this.displays;
    }

    private void placeSingleDisplay(DisplayInfo display, int displaySize) {
        int offset = displaySize / 2;
        int start = -offset;
        int end = (displaySize % 2 == 0) ? offset - 1 : offset;
        for (int i = start; i <= end; i++) {
            for (int j = start; j <= end; j++) {
                int currentX = display.center.x + i;
                int currentY = display.center.y + j;
                if (currentX >= 0 && currentX < m && currentY >= 0 && currentY < n) { // ИСПРАВЛЕНО: Проверка границ m и n
                    matrix[currentY][currentX].type = 2; // ИСПРАВЛЕНО: matrix[y][x]
                    matrix[currentY][currentX].ownerId = display.id;
                }
            }
        }
    }

    public void placeProcessors() {
        Log.info("Этап 1: Максимальное заполнение (с радиусом процессора " + PROCESSOR_REACH + ")");
        Queue<Point2> queue = new LinkedList<>();
        Set<Point2> visited = new HashSet<>();
        List<Point2> genericProcessors = new ArrayList<>();
        
        // ИСПРАВЛЕНО: Правильный обход n (высота, y) и m (ширина, x)
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                if (matrix[y][x].type == 2) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = x + dx;
                            int ny = y + dy;
                            Point2 neighbor = new Point2(nx, ny);
                            if (nx >= 0 && nx < m && ny >= 0 && ny < n && matrix[ny][nx].type == 0 && !visited.contains(neighbor) && isWithinProcessorReachOfAnyDisplay(neighbor)) {
                                queue.add(neighbor);
                                visited.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
        
        while (!queue.isEmpty()) {
            Point2 current = queue.poll();
            matrix[current.y][current.x].type = 1;
            matrix[current.y][current.x].ownerId = -2;
            genericProcessors.add(current);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = current.x + dx;
                    int ny = current.y + dy;
                    Point2 neighbor = new Point2(nx, ny);
                    if (nx >= 0 && nx < m && ny >= 0 && ny < n && matrix[ny][nx].type == 0 && !visited.contains(neighbor) && isWithinProcessorReachOfAnyDisplay(neighbor)) {
                        queue.add(neighbor);
                        visited.add(neighbor);
                    }
                }
            }
        }
        Log.info("Заполнение завершено. Найдено " + genericProcessors.size() + " возможных мест для процессоров.");
        Log.info("Этап 2: Оптимальное распределение процессоров...");
        for (Point2 procPoint : genericProcessors) {
            DisplayInfo bestOwner = null;
            double minDistanceSq = Double.MAX_VALUE;
            for (DisplayInfo display : displays) {
                if (display.getProcessorsNeeded() > 0) {
                    // --- ГЛАВНОЕ ИСПРАВЛЕНИЕ БАГА ---
                    // Вместо display.distanceSq(procPoint) используем правильный метод,
                    // который считает расстояние до КРАЯ прямоугольника.
                    double distSq = distanceSqFromPointToRectangle(procPoint, display);
                    
                    if (distSq <= PROCESSOR_REACH_SQ && distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        bestOwner = display;
                    }
                }
            }
            if (bestOwner != null) {
                matrix[procPoint.y][procPoint.x].ownerId = bestOwner.id;
                bestOwner.processorsPlaced++;
            } else {
                matrix[procPoint.y][procPoint.x].type = 0;
                matrix[procPoint.y][procPoint.x].ownerId = -1;
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