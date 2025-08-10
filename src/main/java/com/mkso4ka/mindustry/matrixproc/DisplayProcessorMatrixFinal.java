package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2;
import arc.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // --- АЛГОРИТМ СТРАТЕГИЧЕСКОГО РАСПРЕДЕЛЕНИЯ С ПРИОРИТЕТОМ ---
    public void placeProcessors() {
        Log.info("Запуск стратегического алгоритма с приоритетом по нужде...");

        // --- ЭТАП 1: Инвентаризация доступных мест для каждого дисплея ---
        Log.info("Этап 1: Инвентаризация всех доступных мест...");
        Map<Integer, List<Spot>> reachableSpotsByDisplay = new HashMap<>();
        boolean[][] isSpotTaken = new boolean[n][m];

        for (DisplayInfo display : displays) {
            List<Spot> spots = new ArrayList<>();
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < m; x++) {
                    if (matrix[y][x].type == 0) { // Если клетка пустая
                        Point2 p = new Point2(x, y);
                        double distSq = distanceSqFromPointToRectangle(p, display);
                        if (distSq <= PROCESSOR_REACH_SQ) {
                            spots.add(new Spot(p, distSq));
                        }
                    }
                }
            }
            // Сортируем места для этого дисплея по расстоянию (от ближайшего к дальнему)
            spots.sort(Comparator.comparingDouble(s -> s.distanceSq));
            reachableSpotsByDisplay.put(display.id, spots);
        }

        // --- ЭТАП 2: Приоритезация дисплеев по "голоду" ---
        Log.info("Этап 2: Приоритезация дисплеев по количеству требуемых процессоров...");
        List<DisplayInfo> sortedDisplays = new ArrayList<>();
        for (DisplayInfo d : displays) {
            sortedDisplays.add(d);
        }
        // Сортируем от самого нуждающегося к наименее
        sortedDisplays.sort(Comparator.comparingInt(DisplayInfo::getProcessorsNeeded).reversed());

        // --- ЭТАП 3: Распределение с учетом приоритета ---
        Log.info("Этап 3: Распределение мест...");
        for (DisplayInfo display : sortedDisplays) {
            int needed = display.getProcessorsNeeded();
            if (needed == 0) continue;

            Log.info(" -> Обслуживаем дисплей " + display.id + " (требуется " + needed + " процессоров)");
            int placedCount = 0;
            List<Spot> potentialSpots = reachableSpotsByDisplay.get(display.id);

            for (Spot spot : potentialSpots) {
                if (placedCount >= needed) {
                    break; // Этот дисплей уже получил всё, что ему нужно
                }

                Point2 loc = spot.location;
                // Если это место еще не занято другим, более приоритетным дисплеем
                if (!isSpotTaken[loc.y][loc.x]) {
                    matrix[loc.y][loc.x].type = 1;
                    matrix[loc.y][loc.x].ownerId = display.id;
                    matrix[loc.y][loc.x].processorIndex = display.processorsPlaced;
                    display.processorsPlaced++;
                    
                    isSpotTaken[loc.y][loc.x] = true; // Занимаем место
                    placedCount++;
                }
            }
            if (placedCount < needed) {
                Log.warn("   -> ВНИМАНИЕ: Дисплей " + display.id + " получил только " + placedCount + " из " + needed + " процессоров. Не хватило физического места.");
            }
        }
        Log.info("Размещение завершено.");
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