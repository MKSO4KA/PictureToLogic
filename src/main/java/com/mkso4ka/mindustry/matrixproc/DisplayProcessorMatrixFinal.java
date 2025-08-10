package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2;
import arc.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class DisplayProcessorMatrixFinal {

    private static class Spot {
        final Point2 location;
        final double distanceSq;
        Spot(Point2 location, double distanceSq) { this.location = location; this.distanceSq = distanceSq; }
    }

    static class Cell {
        int type = 0;
        int ownerId = -1;
        int processorIndex = -1;
    }

    public static final double PROCESSOR_REACH = 10.2;
    private static final double PROCESSOR_REACH_SQ = PROCESSOR_REACH * PROCESSOR_REACH;

    private final int n, m, displaySize;
    private final Cell[][] matrix;
    private final DisplayInfo[] displays;

    public DisplayProcessorMatrixFinal(int n, int m, int[] processorsPerDisplay, int[][] displayBottomLefts, int displaySize) {
        this.n = n;
        this.m = m;
        this.displaySize = displaySize;
        this.matrix = new Cell[n][m];
        for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) matrix[i][j] = new Cell();
        
        this.displays = new DisplayInfo[displayBottomLefts.length];
        for (int i = 0; i < displayBottomLefts.length; i++) {
            Point2 bottomLeft = new Point2(displayBottomLefts[i][0], displayBottomLefts[i][1]);
            displays[i] = new DisplayInfo(i, bottomLeft, processorsPerDisplay[i]);
            placeSingleDisplay(displays[i]);
        }
    }

    public Cell[][] getMatrix() { return this.matrix; }
    public DisplayInfo[] getDisplays() { return this.displays; }

    private void placeSingleDisplay(DisplayInfo display) {
        for (int i = 0; i < displaySize; i++) {
            for (int j = 0; j < displaySize; j++) {
                int currentX = display.bottomLeft.x + j;
                int currentY = display.bottomLeft.y + i;
                if (currentX >= 0 && currentX < m && currentY >= 0 && currentY < n) {
                    matrix[currentY][currentX].type = 2;
                    matrix[currentY][currentX].ownerId = display.id;
                }
            }
        }
    }

    public void placeProcessors() {
        Log.info("Запуск стратегического алгоритма с приоритетом по нужде...");
        List<Point2> allPossibleSpots = new ArrayList<>();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                if (matrix[y][x].type == 0 && isWithinProcessorReachOfAnyDisplay(new Point2(x, y))) {
                    allPossibleSpots.add(new Point2(x, y));
                }
            }
        }
        Log.info("Найдено " + allPossibleSpots.size() + " возможных мест для процессоров.");

        boolean[][] isSpotTaken = new boolean[n][m];
        List<DisplayInfo> sortedDisplays = new ArrayList<>();
        for (DisplayInfo d : displays) sortedDisplays.add(d);
        sortedDisplays.sort(Comparator.comparingInt(DisplayInfo::getProcessorsNeeded).reversed());

        for (DisplayInfo display : sortedDisplays) {
            int needed = display.getProcessorsNeeded();
            if (needed == 0) continue;

            List<Spot> potentialSpots = new ArrayList<>();
            for (Point2 spotLoc : allPossibleSpots) {
                if (!isSpotTaken[spotLoc.y][spotLoc.x]) {
                    double distSq = distanceSqFromPointToRectangle(spotLoc, display);
                    if (distSq <= PROCESSOR_REACH_SQ) {
                        potentialSpots.add(new Spot(spotLoc, distSq));
                    }
                }
            }
            potentialSpots.sort(Comparator.comparingDouble(s -> s.distanceSq));

            int placedCount = 0;
            for (Spot spot : potentialSpots) {
                if (placedCount >= needed) break;
                Point2 loc = spot.location;
                if (!isSpotTaken[loc.y][loc.x]) {
                    matrix[loc.y][loc.x].type = 1;
                    matrix[loc.y][loc.x].ownerId = display.id;
                    matrix[loc.y][loc.x].processorIndex = display.processorsPlaced;
                    display.processorsPlaced++;
                    isSpotTaken[loc.y][loc.x] = true;
                    placedCount++;
                }
            }
            if (placedCount < needed) {
                Log.warn("   -> ВНИМАНИЕ: Дисплей " + display.id + " получил только " + placedCount + " из " + needed + " процессоров. Не хватило физического места.");
            }
        }
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
        int minX = display.bottomLeft.x;
        int maxX = display.bottomLeft.x + displaySize - 1;
        int minY = display.bottomLeft.y;
        int maxY = display.bottomLeft.y + displaySize - 1;
        
        double closestX = Math.max(minX, Math.min(p.x, maxX));
        double closestY = Math.max(minY, Math.min(p.y, maxY));
        
        double dx = p.x - closestX;
        double dy = p.y - closestY;
        
        return dx * dx + dy * dy;
    }
}