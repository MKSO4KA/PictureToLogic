package com.mkso4ka.mindustry.matrixproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

class DisplayProcessorMatrixFinal {
    public static final double PROCESSOR_REACH = 10.2;
    private static final double PROCESSOR_REACH_SQ = PROCESSOR_REACH * PROCESSOR_REACH;

    private final int n;
    private final int m;
    private final Cell[][] matrix;
    private final DisplayInfo[] displays;
    private final int displaySize;

    private static class Cell {
        int type = 0;
        int ownerId = -1;
    }

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
        System.out.println("--- ЭТАП 1: Максимальное заполнение (с радиусом процессора " + PROCESSOR_REACH + ") ---");
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
        System.out.println("Заполнение завершено. Найдено " + genericProcessors.size() + " возможных мест для процессоров.");
        System.out.println("--- ЭТАП 2: Оптимальное распределение процессоров ---");
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
        printFinalStats();
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

    private void printFinalStats() {
        System.out.println("--- Финальная статистика ---");
        int totalRequired = 0;
        int totalPlaced = 0;
        for (DisplayInfo display : displays) {
            totalRequired += display.totalProcessorsRequired;
            totalPlaced += display.processorsPlaced;
        }
        System.out.println("--- ОБЩИЙ ИТОГ: Размещено " + totalPlaced + " из " + totalRequired + " требуемых процессоров. ---");
        for (DisplayInfo display : displays) {
             if (display.getProcessorsNeeded() > 0 && display.processorsPlaced < display.totalProcessorsRequired) {
                System.out.println("Для дисплея " + display.id + " размещено " + display.processorsPlaced + " из " + display.totalProcessorsRequired + " (не хватило места).");
            }
        }
    }

    public void createImage(String filePath) {
        final int CELL_SIZE = 10;
        BufferedImage image = new BufferedImage(m * CELL_SIZE, n * CELL_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, m * CELL_SIZE, n * CELL_SIZE);
        final float GOLDEN_RATIO_CONJUGATE = 0.61803398875f;
        float currentHue = 0.1f;
        Color[] displayColors = new Color[displays.length];
        for (int i = 0; i < displays.length; i++) {
            displayColors[i] = Color.getHSBColor(currentHue, 0.85f, 0.95f);
            currentHue += GOLDEN_RATIO_CONJUGATE;
            currentHue %= 1.0f;
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int cellType = matrix[i][j].type;
                int displayId = matrix[i][j].ownerId;
                Color fillColor = Color.LIGHT_GRAY;
                if (displayId >= 0) {
                    if (cellType == 1) {
                        fillColor = displayColors[displayId];
                    } else if (cellType == 2) {
                        fillColor = displayColors[displayId].darker().darker();
                    }
                }
                g2d.setColor(fillColor);
                g2d.fillRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (matrix[i][j].type == 2) {
                    int myDisplayId = matrix[i][j].ownerId;
                    if (i == 0 || matrix[i - 1][j].ownerId != myDisplayId || matrix[i - 1][j].type != 2) g2d.drawLine(j * CELL_SIZE, i * CELL_SIZE, (j + 1) * CELL_SIZE, i * CELL_SIZE);
                    if (i == n - 1 || matrix[i + 1][j].ownerId != myDisplayId || matrix[i + 1][j].type != 2) g2d.drawLine(j * CELL_SIZE, (i + 1) * CELL_SIZE, (j + 1) * CELL_SIZE, (i + 1) * CELL_SIZE);
                    if (j == 0 || matrix[i][j - 1].ownerId != myDisplayId || matrix[i][j - 1].type != 2) g2d.drawLine(j * CELL_SIZE, i * CELL_SIZE, j * CELL_SIZE, (i + 1) * CELL_SIZE);
                    if (j == m - 1 || matrix[i][j + 1].ownerId != myDisplayId || matrix[i][j + 1].type != 2) g2d.drawLine((j + 1) * CELL_SIZE, i * CELL_SIZE, (j + 1) * CELL_SIZE, (i + 1) * CELL_SIZE);
                }
            }
        }
        g2d.dispose();
        try {
            ImageIO.write(image, "png", new File(filePath));
            System.out.println("Изображение успешно сохранено в " + filePath);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении изображения: " + e.getMessage());
            e.printStackTrace();
        }
    }
}