package com.mkso4ka.mindustry.matrixproc;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import io.github.jdiemke.delaunator.Delaunator;
import io.github.jdiemke.delaunator.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ImageProcessor {
    private final Pixmap originalPixmap;
    private int width;
    private int height;

    // --- ИЗМЕНЕНО: ProcessingSteps теперь хранит треугольники ---
    public static class ProcessingSteps {
        public final Map<Integer, List<Triangle>> result;
        public ProcessingSteps(Map<Integer, List<Triangle>> result) {
            this.result = result;
        }
    }

    // --- ИЗМЕНЕНО: Новый класс для хранения треугольников ---
    public static class Triangle {
        final int x1, y1, x2, y2, x3, y3;
        Triangle(int x1, int y1, int x2, int y2, int x3, int y3) {
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.x3 = x3; this.y3 = y3;
        }
    }

    public ImageProcessor(Pixmap pixmap) {
        this.originalPixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
    }

    // --- ИЗМЕНЕНО: Главный метод теперь вызывает триангуляцию ---
    public ProcessingSteps process(double tolerance, int diffusionIterations, float diffusionContrast) {
        // Tolerance теперь управляет количеством точек (детализацией)
        // 0.0 -> ~5000 точек, 3.0 -> ~500 точек
        int maxPoints = (int)(5000 - tolerance * 1500);

        // 1. Находим грани с помощью оператора Собеля
        float[][] edgeMap = sobelEdgeDetect(originalPixmap);

        // 2. Расставляем точки
        List<Point> points = placePoints(edgeMap, maxPoints);

        // 3. Выполняем триангуляцию Делоне
        Delaunator delaunator = new Delaunator(points);

        // 4. Раскрашиваем треугольники и группируем по цвету
        Map<Integer, List<Triangle>> trianglesByColor = colorTriangles(delaunator, points);
        
        WebLogger.info("Triangulation complete. Generated %d triangles.", delaunator.triangles.length / 3);

        return new ProcessingSteps(trianglesByColor);
    }

    // --- НОВЫЕ МЕТОДЫ ДЛЯ ТРИАНГУЛЯЦИИ ---

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

        // Нормализуем карту граней
        if (maxGradient > 0) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    edgeMap[x][y] /= maxGradient;
                }
            }
        }
        return edgeMap;
    }

    private List<Point> placePoints(float[][] edgeMap, int maxPoints) {
        List<Point> points = new ArrayList<>();
        Random random = new Random(0);

        // Обязательно добавляем углы и середины сторон
        points.add(new Point(0, 0));
        points.add(new Point(width - 1, 0));
        points.add(new Point(0, height - 1));
        points.add(new Point(width - 1, height - 1));
        points.add(new Point(width / 2, 0));
        points.add(new Point(width / 2, height - 1));
        points.add(new Point(0, height / 2));
        points.add(new Point(width - 1, height / 2));

        // Добавляем точки в зависимости от "важности" (яркости на карте граней)
        for (int i = 0; i < maxPoints * 10 && points.size() < maxPoints; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            if (random.nextFloat() < edgeMap[x][y]) {
                points.add(new Point(x, y));
            }
        }
        // Если точек все еще мало, добавляем случайные
        while (points.size() < maxPoints) {
            points.add(new Point(random.nextInt(width), random.nextInt(height)));
        }
        return points;
    }

    private Map<Integer, List<Triangle>> colorTriangles(Delaunator delaunator, List<Point> points) {
        Map<Integer, List<Triangle>> trianglesByColor = new HashMap<>();
        int[] triangles = delaunator.triangles;

        for (int i = 0; i < triangles.length; i += 3) {
            Point p1 = points.get(triangles[i]);
            Point p2 = points.get(triangles[i + 1]);
            Point p3 = points.get(triangles[i + 2]);

            // Находим центр треугольника, чтобы взять оттуда цвет
            int centerX = (int)(p1.x + p2.x + p3.x) / 3;
            int centerY = (int)(p1.y + p2.y + p3.y) / 3;
            
            // Убедимся, что центр внутри изображения
            centerX = Math.max(0, Math.min(width - 1, centerX));
            centerY = Math.max(0, Math.min(height - 1, centerY));

            int color = originalPixmap.get(centerX, centerY);
            
            if ((color & 0xff) > 10) { // Игнорируем почти прозрачные
                Triangle t = new Triangle((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y, (int)p3.x, (int)p3.y);
                trianglesByColor.computeIfAbsent(color, k -> new ArrayList<>()).add(t);
            }
        }
        return trianglesByColor;
    }
}
