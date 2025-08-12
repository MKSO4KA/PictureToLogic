package com.mkso4ka.mindustry.matrixproc;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import org.waveware.delaunator.Delaunator;
import org.waveware.delaunator.DPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ImageProcessor {
    private final Pixmap originalPixmap;
    private int width;
    private int height;

    // --- Вспомогательные классы ---
    public static class ProcessingSteps {
        public final Map<Integer, List<Triangle>> result;
        public ProcessingSteps(Map<Integer, List<Triangle>> result) {
            this.result = result;
        }
    }

    public static class Triangle {
        final int x1, y1, x2, y2, x3, y3;
        Triangle(int x1, int y1, int x2, int y2, int x3, int y3) {
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.x3 = x3; this.y3 = y3;
        }
    }

    // --- Конструктор ---
    public ImageProcessor(Pixmap pixmap) {
        this.originalPixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
    }
    
    // --- Главный метод обработки ---
    public ProcessingSteps process(double tolerance, int diffusionIterations, float diffusionContrast, int displayId) {
        int maxPoints = (int)(5000 - tolerance * 1500);

        float[][] edgeMap = sobelEdgeDetect(originalPixmap);
        logEdgeMap(edgeMap, displayId);

        List<DPoint> points = placePoints(edgeMap, maxPoints);
        logPlacedPoints(points, displayId);

        Delaunator delaunator = new Delaunator(points);
        Map<Integer, List<Triangle>> trianglesByColor = colorTriangles(delaunator, points);
        logFinalTriangulation(trianglesByColor, displayId);
        
        WebLogger.info("Triangulation for display #%d complete. Generated %d triangles.", displayId, delaunator.triangles.length / 3);
        return new ProcessingSteps(trianglesByColor);
    }

    // --- Методы логирования промежуточных этапов ---
    private void logEdgeMap(float[][] edgeMap, int displayId) {
        Pixmap edgePixmap = new Pixmap(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = (int)(edgeMap[x][y] * 255);
                edgePixmap.set(x, y, Color.rgba8888(gray, gray, gray, 255));
            }
        }
        WebLogger.logImage(String.format("slice_%d_1_EdgeMap", displayId), edgePixmap);
        edgePixmap.dispose();
    }

    private void logPlacedPoints(List<DPoint> points, int displayId) {
        Pixmap pointsPixmap = originalPixmap.copy();
        int greenColor = Color.green.rgba();
        for (DPoint p : points) {
            // Рисуем квадрат 2x2 вместо точки для лучшей видимости
            fillRectangleManually(pointsPixmap, (int) p.x, (int) p.y, 2, 2, greenColor);
        }
        WebLogger.logImage(String.format("slice_%d_2_PlacedPoints", displayId), pointsPixmap);
        pointsPixmap.dispose();
    }
    
    private void logFinalTriangulation(Map<Integer, List<Triangle>> trianglesByColor, int displayId) {
        Pixmap finalTrianglesPixmap = new Pixmap(width, height);
        // Устанавливаем режим смешивания в 0 (соответствует Blending.none)
        finalTrianglesPixmap.setBlending(0); 
        
        for (Map.Entry<Integer, List<Triangle>> entry : trianglesByColor.entrySet()) {
            int color = entry.getKey();
            for (Triangle t : entry.getValue()) {
                fillTriangleManually(finalTrianglesPixmap, t.x1, t.y1, t.x2, t.y2, t.x3, t.y3, color);
            }
        }
        WebLogger.logImage(String.format("slice_%d_3_FinalTriangulation", displayId), finalTrianglesPixmap);
        finalTrianglesPixmap.dispose();
    }

    // --- Ручные реализации методов рисования (для совместимости) ---

    private void fillRectangleManually(Pixmap pixmap, int x, int y, int w, int h, int color) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                if (i >= 0 && i < pixmap.getWidth() && j >= 0 && j < pixmap.getHeight()) {
                    pixmap.set(i, j, color);
                }
            }
        }
    }

    private void fillTriangleManually(Pixmap pixmap, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        // Сортируем вершины по Y-координате (v1 - самая верхняя)
        int[][] vertices = {{x1, y1}, {x2, y2}, {x3, y3}};
        Arrays.sort(vertices, Comparator.comparingInt(v -> v[1]));
        int[] v1 = vertices[0], v2 = vertices[1], v3 = vertices[2];

        // Частный случай: горизонтальная нижняя сторона
        if (v2[1] == v3[1]) {
            fillFlatBottomTriangle(pixmap, v1, v2, v3, color);
        } 
        // Частный случай: горизонтальная верхняя сторона
        else if (v1[1] == v2[1]) {
            fillFlatTopTriangle(pixmap, v1, v2, v3, color);
        } 
        // Общий случай: разбиваем на 2 треугольника
        else {
            int[] v4 = {(int)(v1[0] + ((float)(v2[1] - v1[1]) / (float)(v3[1] - v1[1])) * (v3[0] - v1[0])), v2[1]};
            fillFlatBottomTriangle(pixmap, v1, v2, v4, color);
            fillFlatTopTriangle(pixmap, v2, v4, v3, color);
        }
    }

    private void fillFlatBottomTriangle(Pixmap pixmap, int[] v1, int[] v2, int[] v3, int color) {
        float invSlope1 = (float)(v2[0] - v1[0]) / (v2[1] - v1[1]);
        float invSlope2 = (float)(v3[0] - v1[0]) / (v3[1] - v1[1]);
        float curX1 = v1[0];
        float curX2 = v1[0];
        for (int scanlineY = v1[1]; scanlineY <= v2[1]; scanlineY++) {
            drawHorizontalLine(pixmap, (int)curX1, (int)curX2, scanlineY, color);
            curX1 += invSlope1;
            curX2 += invSlope2;
        }
    }

    private void fillFlatTopTriangle(Pixmap pixmap, int[] v1, int[] v2, int[] v3, int color) {
        float invSlope1 = (float)(v3[0] - v1[0]) / (v3[1] - v1[1]);
        float invSlope2 = (float)(v3[0] - v2[0]) / (v3[1] - v2[1]);
        float curX1 = v3[0];
        float curX2 = v3[0];
        for (int scanlineY = v3[1]; scanlineY > v1[1]; scanlineY--) {
            drawHorizontalLine(pixmap, (int)curX1, (int)curX2, scanlineY, color);
            curX1 -= invSlope1;
            curX2 -= invSlope2;
        }
    }

    private void drawHorizontalLine(Pixmap pixmap, int x1, int x2, int y, int color) {
        int startX = Math.min(x1, x2);
        int endX = Math.max(x1, x2);
        for (int x = startX; x <= endX; x++) {
            if (x >= 0 && x < pixmap.getWidth() && y >= 0 && y < pixmap.getHeight()) {
                pixmap.set(x, y, color);
            }
        }
    }

    // --- Алгоритмы обработки изображения (без изменений) ---

    private float[][] sobelEdgeDetect(Pixmap source) {
        float[][] gray = new float[width][height];
        float[][] edgeMap = new float[width][height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = source.get(x, y);
                int r = (c >> 24) & 0xff;
                int g = (c >> 16) & 0xff;
                int b = (c >> 8) & 0xff;
                gray[x][y] = (r * 0.299f + g * 0.587f + b * 0.114f);
            }
        }

        float maxGradient = -1;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float gx = (gray[x+1][y-1] + 2*gray[x+1][y] + gray[x+1][y+1]) - (gray[x-1][y-1] + 2*gray[x-1][y] + gray[x-1][y+1]);
                float gy = (gray[x-1][y+1] + 2*gray[x][y+1] + gray[x+1][y+1]) - (gray[x-1][y-1] + 2*gray[x][y-1] + gray[x+1][y-1]);
                float g = (float)Math.sqrt(gx*gx + gy*gy);
                edgeMap[x][y] = g;
                if (g > maxGradient) maxGradient = g;
            }
        }

        if (maxGradient > 0) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    edgeMap[x][y] /= maxGradient;
                }
            }
        }
        return edgeMap;
    }

    private List<DPoint> placePoints(float[][] edgeMap, int maxPoints) {
        List<DPoint> points = new ArrayList<>();
        Random random = new Random(0);

        points.add(new DPoint(0, 0));
        points.add(new DPoint(width - 1, 0));
        points.add(new DPoint(0, height - 1));
        points.add(new DPoint(width - 1, height - 1));
        points.add(new DPoint(width / 2, 0));
        points.add(new DPoint(width / 2, height - 1));
        points.add(new DPoint(0, height / 2));
        points.add(new DPoint(width - 1, height / 2));

        for (int i = 0; i < maxPoints * 10 && points.size() < maxPoints; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            if (random.nextFloat() < edgeMap[x][y]) {
                points.add(new DPoint(x, y));
            }
        }
        while (points.size() < maxPoints) {
            points.add(new DPoint(random.nextInt(width), random.nextInt(height)));
        }
        return points;
    }

    private Map<Integer, List<Triangle>> colorTriangles(Delaunator delaunator, List<DPoint> points) {
        Map<Integer, List<Triangle>> trianglesByColor = new HashMap<>();
        int[] triangles = delaunator.triangles;

        for (int i = 0; i < triangles.length; i += 3) {
            DPoint p1 = points.get(triangles[i]);
            DPoint p2 = points.get(triangles[i + 1]);
            DPoint p3 = points.get(triangles[i + 2]);

            int centerX = (int)(p1.x + p2.x + p3.x) / 3;
            int centerY = (int)(p1.y + p2.y + p3.y) / 3;
            
            centerX = Math.max(0, Math.min(width - 1, centerX));
            centerY = Math.max(0, Math.min(height - 1, centerY));

            int color = originalPixmap.get(centerX, centerY);
            
            if ((color & 0xff) > 10) { 
                Triangle t = new Triangle((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y, (int)p3.x, (int)p3.y);
                trianglesByColor.computeIfAbsent(color, k -> new ArrayList<>()).add(t);
            }
        }
        return trianglesByColor;
    }
}