package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2;
import arc.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// ИСПРАВЛЕНО: Убраны лишние импорты, которые больше не нужны
// import java.util.Queue;
// import java.util.LinkedList;
// import java.util.HashMap;
// import java.util.Map;

class DisplayProcessorMatrixFinal {

    // Вспомогательный класс для хранения точки и расстояния до нее
    private static class Spot {
        final Point2 location;
        final double distanceSq;

        Spot(Point2 location, double distanceSq) {
            this.location = location;
            this.distanceSq = distanceSq;
        }
    }

    static class Cell {
        int type = 0;
        int ownerId = -1;
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

    // --- ФИНАЛЬНЫЙ АЛГОРИТМ: СТРАТЕГИЧЕСКОЕ РАЗМЕЩЕНИЕ ТОЛЬКО НЕОБХОДИМОГО ---
    public void placeProcessors() {
        Log.info("Запуск стратегического алгоритма с приоритетом по нужде...");

        // --- ЭТАП 1: Инвентаризация всех доступных мест ---
        Log.info("Этап 1: Инвентаризация всех доступных мест...");
        List<Point2> allPossibleSpots = new ArrayList<>();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                if (matrix[y][x].type == 0 && isWithinProcessorReachOfAnyDisplay(new Point2(x, y))) {
                    allPossibleSpots.add(new Point2(x, y));
                }
            }
        }
        Log.info("Найдено " + allPossibleSpots.size() + " возможных мест для процессоров.");

        // --- ЭТАП 2: Приоритетное распределение для удовлетворения нужд ---
        Log.info("Этап 2: Удовлетворение гарантированных потребностей...");
        boolean[][] isSpotTaken = new boolean[n][m];
        List<DisplayInfo> sortedDisplays = new ArrayList<>();
        for (DisplayInfo d : displays) sortedDisplays.add(d);
        sortedDisplays.sort(Comparator.comparingInt(DisplayInfo::getProcessorsNeeded).reversed());

        for (DisplayInfo display : sortedDisplays) {
            int needed = display.getProcessorsNeeded();
            if (needed == 0) continue;

            // Собираем список всех доступных и подходящих для *этого* дисплея мест
            List<Spot> potentialSpots = new ArrayList<>();
            for (Point2 spotLoc : allPossibleSpots) {
                if (!isSpotTaken[spotLoc.y][spotLoc.x]) {
                    double distSq = distanceSqFromPointToRectangle(spotLoc, display);
                    if (distSq <= PROCESSOR_REACH_SQ) {
                        potentialSpots.add(new Spot(spotLoc, distSq));
                    }
                }
            }
            // Сортируем их, чтобы выбрать лучшие (ближайшие)
            potentialSpots.sort(Comparator.comparingDouble(s -> s.distanceSq));

            int placedCount = 0;
            for (Spot spot : potentialSpots) {
                if (placedCount >= needed) break;
                
                Point2 loc = spot.location;
                // Двойная проверка, что место не занято (на случай гонки)
                if (!isSpotTaken[loc.y][loc.x]) {
                    matrix[loc.y][loc.x].type = 1;
                    matrix[loc.y][loc.x].ownerId = display.id;
                    matrix[loc.y][loc.x].processorIndex = display.processorsPlaced;
                    display.processorsPlaced++;
                    
                    isSpotTaken[loc.y][loc.x] = true; // Занимаем место глобально
                    placedCount++;
                }
            }
            if (placedCount < needed) {
                Log.warn("   -> ВНИМАНИЕ: Дисплей " + display.id + " получил только " + placedCount + " из " + needed + " процессоров. Не хватило физического места.");
            }
        }
        
        // --- ЭТАП 3 УДАЛЕН ---
        // Мы больше не заполняем оставшиеся места. Алгоритм завершается здесь.
        Log.info("Размещение требуемых процессоров завершено.");
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