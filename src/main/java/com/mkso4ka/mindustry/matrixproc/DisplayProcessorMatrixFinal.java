package com.mkso4ka.mindustry.matrixproc;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import arc.util.Log; // Используем логирование из Arc

// ВАЖНО: Добавляем внутренний класс для хранения информации о клетке
class Cell {
    int type = 0; // 0 = пусто, 1 = процессор, 2 = дисплей
    int ownerId = -1; // ID дисплея, к которому привязан процессор/дисплей
}

class DisplayProcessorMatrixFinal {
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
            Point center = new Point(displayCenters[i][0], displayCenters[i][1]);
            displays[i] = new DisplayInfo(i, center, processorsPerDisplay[i]);
            placeSingleDisplay(displays[i], displaySize);
        }
    }

    // --- ИЗМЕНЕНИЕ: Делаем метод публичным, чтобы его мог видеть LogicCore ---
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
                if (currentX >= 0 && currentX < n && currentY >= 0 && currentY < m) {
                    matrix[currentX][currentY].type = 2;
                    matrix[currentX][currentY].ownerId = display.id;
                }
            }
        }
    }

    public void placeProcessors() {
        Log.info("Этап 1: Максимальное заполнение...");
        Queue<Point> queue = new LinkedList<>();
        Set<Point> visited = new HashSet<>();
        List<Point> genericProcessors = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (matrix[i][j].type == 2) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = i + dx;
                            int ny = j + dy;
                            Point neighbor = new Point(nx, ny);
                            if (nx >= 0 && nx < n && ny >= 0 && ny < m && matrix[nx][ny].type == 0 && !visited.contains(neighbor) && isWithinProcessorReachOfAnyDisplay(neighbor)) {
                                queue.add(neighbor);
                                visited.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            Point current = queue.poll();
            matrix[current.x][current.y].type = 1;
            matrix[current.x][current.y].ownerId = -2;
            genericProcessors.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = current.x + dx;
                    int ny = current.y + dy;
                    Point neighbor = new Point(nx, ny);
                    if (nx >= 0 && nx < n && ny >= 0 && ny < m && matrix[nx][ny].type == 0 && !visited.contains(neighbor) && isWithinProcessorReachOfAnyDisplay(neighbor)) {
                        queue.add(neighbor);
                        visited.add(neighbor);
                    }
                }
            }
        }
        Log.info("Завершено. Найдено " + genericProcessors.size() + " мест.");
        Log.info("Этап 2: Оптимальное распределение...");
        for (Point procPoint : genericProcessors) {
            DisplayInfo bestOwner = null;
            double minDistanceSq = Double.MAX_VALUE;
            for (DisplayInfo display : displays) {
                if (display.getProcessorsNeeded() > 0) {
                    double distSq = display.distanceSq(procPoint);
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        bestOwner = display;
                    }
                }
            }
            if (bestOwner != null) {
                matrix[procPoint.x][procPoint.y].ownerId = bestOwner.id;
                bestOwner.processorsPlaced++;
            } else {
                matrix[procPoint.x][procPoint.y].type = 0;
                matrix[procPoint.x][procPoint.y].ownerId = -1;
            }
        }
    }

    private boolean isWithinProcessorReachOfAnyDisplay(Point p) {
        for (DisplayInfo display : displays) {
            if (distanceSqFromPointToRectangle(p, display) <= PROCESSOR_REACH_SQ) {
                return true;
            }
        }
        return false;
    }

    private double distanceSqFromPointToRectangle(Point p, DisplayInfo display) {
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