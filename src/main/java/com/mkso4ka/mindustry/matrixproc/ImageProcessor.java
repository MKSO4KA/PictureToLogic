package com.mkso4ka.mindustry.matrixproc;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import org.waveware.delaunator.Delaunator;
import org.waveware.delaunator.DPoint;

// Импорты для новой библиотеки
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;
import java.io.ByteArrayOutputStream;

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

    public static class ProcessingSteps {
        public final Map<Integer, List<Triangle>> result;
        public ProcessingSteps(Map<Integer, List<Triangle>> result) { this.result = result; }
    }
    public static class Triangle {
        final int x1, y1, x2, y2, x3, y3;
        Triangle(int x1, int y1, int x2, int y2, int x3, int y3) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.x3 = x3; this.y3 = y3;
        }
    }
    
    public ImageProcessor(Pixmap pixmap) {
        this.originalPixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
    }
    
    public ProcessingSteps process(double detail, int displayId) {
        // Теперь чем выше detail, тем БОЛЬШЕ точек
        int maxPoints = (int)(500 + detail * 1500);

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
    
    // --- Новые методы логирования с использованием PNGJ ---

    private void logPlacedPoints(List<DPoint> points, int displayId) {
        int[][] canvas = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                canvas[y][x] = originalPixmap.get(x, y);
            }
        }
        
        int greenColor = Color.green.rgba();
        for(DPoint p : points) {
            fillRectangleManually(canvas, (int) p.x, (int) p.y, 2, 2, greenColor);
        }
        
        byte[] pngBytes = encodeToPng(canvas);
        WebLogger.logImage(String.format("slice_%d_2_PlacedPoints", displayId), pngBytes);
    }
    
    private void logFinalTriangulation(Map<Integer, List<Triangle>> trianglesByColor, int displayId) {
        int[][] canvas = new int[height][width]; 
        
        for (Map.Entry<Integer, List<Triangle>> entry : trianglesByColor.entrySet()) {
            int color = entry.getKey();
            for (Triangle t : entry.getValue()) {
                fillTriangleManually(canvas, t.x1, t.y1, t.x2, t.y2, t.x3, t.y3, color);
            }
        }
        
        byte[] pngBytes = encodeToPng(canvas);
        WebLogger.logImage(String.format("slice_%d_3_FinalTriangulation", displayId), pngBytes);
    }
    
    private byte[] encodeToPng(int[][] canvas) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageInfo imi = new ImageInfo(width, height, 8, true); 
        PngWriter pngw = new PngWriter(baos, imi);
        
        int[] row = new int[width * 4]; 
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = canvas[y][x];
                row[x * 4 + 0] = (rgba >> 24) & 0xFF; // R
                row[x * 4 + 1] = (rgba >> 16) & 0xFF; // G
                row[x * 4 + 2] = (rgba >> 8) & 0xFF;  // B
                row[x * 4 + 3] = rgba & 0xFF;         // A
            }
            pngw.writeRowInt(row);
        }
        pngw.end();
        return baos.toByteArray();
    }

    // --- Методы ручной растеризации (рисования) на 2D-массиве ---
    private void fillRectangleManually(int[][] canvas, int x, int y, int w, int h, int color) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                if (i >= 0 && i < width && j >= 0 && j < height) {
                    canvas[j][i] = color;
                }
            }
        }
    }
    
    private void fillTriangleManually(int[][] canvas, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        int[][] vertices = {{x1, y1}, {x2, y2}, {x3, y3}};
        Arrays.sort(vertices, Comparator.comparingInt(v -> v[1]));
        int[] v1 = vertices[0], v2 = vertices[1], v3 = vertices[2];
        if (v2[1] == v3[1]) { fillFlatBottomTriangle(canvas, v1, v2, v3, color); }
        else if (v1[1] == v2[1]) { fillFlatTopTriangle(canvas, v1, v2, v3, color); }
        else {
            int[] v4 = {(int)(v1[0] + ((float)(v2[1] - v1[1]) / (float)(v3[1] - v1[1])) * (v3[0] - v1[0])), v2[1]};
            fillFlatBottomTriangle(canvas, v1, v2, v4, color);
            fillFlatTopTriangle(canvas, v2, v4, v3, color);
        }
    }
    
    private void fillFlatBottomTriangle(int[][] canvas, int[] v1, int[] v2, int[] v3, int color) {
        if (v1[1] == v2[1]) return;
        float invSlope1 = (float)(v2[0] - v1[0]) / (v2[1] - v1[1]);
        float invSlope2 = (float)(v3[0] - v1[0]) / (v3[1] - v1[1]);
        float curX1 = v1[0]; float curX2 = v1[0];
        for (int scanlineY = v1[1]; scanlineY <= v2[1]; scanlineY++) {
            drawHorizontalLine(canvas, (int)curX1, (int)curX2, scanlineY, color);
            curX1 += invSlope1; curX2 += invSlope2;
        }
    }

    private void fillFlatTopTriangle(int[][] canvas, int[] v1, int[] v2, int[] v3, int color) {
        if (v1[1] == v3[1] || v2[1] == v3[1]) return;
        float invSlope1 = (float)(v3[0] - v1[0]) / (v3[1] - v1[1]);
        float invSlope2 = (float)(v3[0] - v2[0]) / (v3[1] - v2[1]);
        float curX1 = v3[0]; float curX2 = v3[0];
        for (int scanlineY = v3[1]; scanlineY > v1[1]; scanlineY--) {
            drawHorizontalLine(canvas, (int)curX1, (int)curX2, scanlineY, color);
            curX1 -= invSlope1; curX2 -= invSlope2;
        }
    }
    
    private void drawHorizontalLine(int[][] canvas, int x1, int x2, int y, int color) {
        if (y < 0 || y >= height) return;
        int startX = Math.max(0, Math.min(x1, x2));
        int endX = Math.min(width - 1, Math.max(x1, x2));
        for (int x = startX; x <= endX; x++) {
            canvas[y][x] = color;
        }
    }
    
    private void logEdgeMap(float[][] edgeMap, int displayId) {
        Pixmap edgePixmap = new Pixmap(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Умножаем на 5, чтобы сделать границы ярче на отладочном изображении
                int gray = (int)(Math.min(1.0f, edgeMap[x][y] * 5.0f) * 255);
                edgePixmap.set(x, y, Color.rgba8888(gray, gray, gray, 255));
            }
        }
        WebLogger.logImage(String.format("slice_%d_1_EdgeMap", displayId), edgePixmap);
        edgePixmap.dispose();
    }

    private float[][] sobelEdgeDetect(Pixmap source) {
        float[][] gray = new float[width][height]; float[][] edgeMap = new float[width][height];
        for (int y = 0; y < height; y++) { for (int x = 0; x < width; x++) {
            int c = source.get(x, y); int r = (c >> 24) & 0xff; int g = (c >> 16) & 0xff; int b = (c >> 8) & 0xff;
            gray[x][y] = (r * 0.299f + g * 0.587f + b * 0.114f);
        }}
        float maxGradient = -1;
        for (int y = 1; y < height - 1; y++) { for (int x = 1; x < width - 1; x++) {
            float gx = (gray[x+1][y-1] + 2*gray[x+1][y] + gray[x+1][y+1]) - (gray[x-1][y-1] + 2*gray[x-1][y] + gray[x-1][y+1]);
            float gy = (gray[x-1][y+1] + 2*gray[x][y+1] + gray[x+1][y+1]) - (gray[x-1][y-1] + 2*gray[x][y-1] + gray[x+1][y-1]);
            float g = (float)Math.sqrt(gx*gx + gy*gy); edgeMap[x][y] = g;
            if (g > maxGradient) maxGradient = g;
        }}
        if (maxGradient > 0) { for (int y = 0; y < height; y++) { for (int x = 0; x < width; x++) {
            edgeMap[x][y] /= maxGradient;
        }}} return edgeMap;
    }

    private List<DPoint> placePoints(float[][] edgeMap, int maxPoints) {
        List<DPoint> points = new ArrayList<>();
        Random random = new Random(0); // Используем фиксированный seed для повторяемости

        // 1. Добавляем обязательные точки по краям и в центре, чтобы гарантировать охват всего изображения
        points.add(new DPoint(0, 0));
        points.add(new DPoint(width - 1, 0));
        points.add(new DPoint(0, height - 1));
        points.add(new DPoint(width - 1, height - 1));
        points.add(new DPoint(width / 2, 0));
        points.add(new DPoint(width / 2, height - 1));
        points.add(new DPoint(0, height / 2));
        points.add(new DPoint(width - 1, height / 2));

        // 2. Собираем все точки-кандидаты, которые лежат на границах
        List<DPoint> edgeCandidates = new ArrayList<>();
        // Порог можно настроить. 0.1 - хорошее начало.
        // Чем он ниже, тем более слабые границы будут учитываться.
        float threshold = 0.1f; 
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (edgeMap[x][y] > threshold) {
                    edgeCandidates.add(new DPoint(x, y));
                }
            }
        }
        WebLogger.info("Найдено %d точек-кандидатов на границах (порог > %.2f)", edgeCandidates.size(), threshold);

        // 3. Перемешиваем кандидатов и добавляем нужное количество в итоговый список
        java.util.Collections.shuffle(edgeCandidates, random);
        int pointsToAdd = Math.min(maxPoints - points.size(), edgeCandidates.size());
        if (pointsToAdd > 0) {
            points.addAll(edgeCandidates.subList(0, pointsToAdd));
        }

        // 4. Если точек все еще не хватает (например, на почти пустом изображении), 
        // добиваем случайными, как и раньше.
        while (points.size() < maxPoints) {
            points.add(new DPoint(random.nextInt(width), random.nextInt(height)));
        }

        return points;
    }

    private Map<Integer, List<Triangle>> colorTriangles(Delaunator delaunator, List<DPoint> points) {
        Map<Integer, List<Triangle>> trianglesByColor = new HashMap<>(); int[] triangles = delaunator.triangles;
        for (int i = 0; i < triangles.length; i += 3) {
            DPoint p1 = points.get(triangles[i]); DPoint p2 = points.get(triangles[i + 1]); DPoint p3 = points.get(triangles[i + 2]);
            int centerX = (int)(p1.x + p2.x + p3.x) / 3; int centerY = (int)(p1.y + p2.y + p3.y) / 3;
            centerX = Math.max(0, Math.min(width - 1, centerX)); centerY = Math.max(0, Math.min(height - 1, centerY));
            int color = originalPixmap.get(centerX, centerY);
            if ((color & 0xff) > 10) { 
                Triangle t = new Triangle((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y, (int)p3.x, (int)p3.y);
                trianglesByColor.computeIfAbsent(color, k -> new ArrayList<>()).add(t);
            }
        } return trianglesByColor;
    }
}