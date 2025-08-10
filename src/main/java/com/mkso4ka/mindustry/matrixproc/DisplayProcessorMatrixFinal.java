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
                int currentX = display.center.x + j; // X - это j
                int currentY = display.center.y + i; // Y - это i
                if (currentX >= 0 && currentX < m && currentY >= 0 && currentY < n) {
                    matrix[currentY][currentX].type = 2;
                    matrix[currentY][currentX].ownerId = display.id;
                }
            }
        }
    }

    // --- ПОЛНОСТЬЮ ПЕРЕПИСАННЫЙ АЛГОРИТМ РАЗМЕЩЕНИЯ ---
    public void placeProcessors() {
        // --- ЭТАП 1: Поиск всех возможных мест для процессоров (остается без изменений) ---
        Log.info("Этап 1: Поиск всех возможных мест для процессоров...");
        Queue<Point2> queue = new LinkedList<>();
        Set<Point2> visited = new HashSet<>();
        List<Point2> genericProcessors = new ArrayList<>();
        
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
            matrix[current.y][current.x].ownerId = -2; // Временно помечаем как "общий"
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
        Log.info("Завершено. Найдено " + genericProcessors.size() + " возможных мест.");

        // --- ЭТАП 2: Дисплей-центричное распределение для удовлетворения потребностей ---
        Log.info("Этап 2: Приоритетное распределение требуемых процессоров...");
        List<Point2> availableProcLocations = new ArrayList<>(genericProcessors);

        for (DisplayInfo display : displays) {
            while (display.getProcessorsNeeded() > 0) {
                Point2 bestSpot = null;
                double minDistanceSq = Double.MAX_VALUE;

                // Для текущего дисплея ищем лучший из ВСЕХ ЕЩЕ ДОСТУПНЫХ процессоров
                for (Point2 spot : availableProcLocations) {
                    double distSq = distanceSqFromPointToRectangle(spot, display);
                    if (distSq <= PROCESSOR_REACH_SQ && distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        bestSpot = spot;
                    }
                }

                if (bestSpot != null) {
                    // Мы нашли лучший спот для этого дисплея. Забираем его.
                    matrix[bestSpot.y][bestSpot.x].ownerId = display.id;
                    display.processorsPlaced++;
                    // Удаляем его из списка доступных, чтобы другие не могли его забрать
                    availableProcLocations.remove(bestSpot);
                } else {
                    // Если для дисплея не нашлось ни одного доступного места,
                    // прекращаем попытки для него.
                    Log.warn("Для дисплея " + display.id + " не найдено доступных мест в радиусе досягаемости.");
                    break; 
                }
            }
        }

        // --- ЭТАП 3: Очистка неиспользованных процессоров ---
        Log.info("Этап 3: Очистка нераспределенных процессоров...");
        for (Point2 unusedSpot : availableProcLocations) {
            // Если процессор остался в списке, значит, он не понадобился ни одному дисплею.
            matrix[unusedSpot.y][unusedSpot.x].type = 0;
            matrix[unusedSpot.y][unusedSpot.x].ownerId = -1;
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