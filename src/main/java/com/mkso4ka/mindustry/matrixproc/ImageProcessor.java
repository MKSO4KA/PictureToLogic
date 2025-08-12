package com.mkso4ka.mindustry.matrixproc;

import arc.graphics.Pixmap;
import org.waveware.delaunator.Delaunator;
import org.waveware.delaunator.DPoint;

// Импортируем стандартные классы Java для работы с графикой
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
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

    public ImageProcessor(Pixmap pixmap) {
        this.originalPixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
    }
    
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

    // --- НОВЫЕ, НАДЕЖНЫЕ МЕТОДЫ ЛОГИРОВАНИЯ с java.awt ---

    private void logPlacedPoints(List<DPoint> points, int displayId) {
        // Создаем стандартное Java-изображение
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Копируем исходное изображение в BufferedImage
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = originalPixmap.get(x, y);
                // Конвертируем формат arc RGBA8888 в формат Java ARGB
                int r = (rgba >> 24) & 0xff;
                int g = (rgba >> 16) & 0xff;
                int b = (rgba >> 8) & 0xff;
                int a = rgba & 0xff;
                image.setRGB(x, y, new Color(r, g, b, a).getRGB());
            }
        }

        // Рисуем точки поверх изображения
        g2d.setColor(java.awt.Color.GREEN);
        for (DPoint p : points) {
            g2d.fillRect((int) p.x, (int) p.y, 2, 2);
        }
        g2d.dispose();

        // Конвертируем обратно в Pixmap и логируем
        WebLogger.logImage(String.format("slice_%d_2_PlacedPoints", displayId), bufferedImageToPixmap(image));
    }
    
    private void logFinalTriangulation(Map<Integer, List<Triangle>> trianglesByColor, int displayId) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        for (Map.Entry<Integer, List<Triangle>> entry : trianglesByColor.entrySet()) {
            int rgba = entry.getKey();
            int r = (rgba >> 24) & 0xff;
            int g = (rgba >> 16) & 0xff;
            int b = (rgba >> 8) & 0xff;
            int a = rgba & 0xff;
            g2d.setColor(new Color(r, g, b, a));

            for (Triangle t : entry.getValue()) {
                Polygon polygon = new Polygon(
                    new int[]{t.x1, t.x2, t.x3},
                    new int[]{t.y1, t.y2, t.y3},
                    3
                );
                g2d.fillPolygon(polygon);
            }
        }
        g2d.dispose();
        
        WebLogger.logImage(String.format("slice_%d_3_FinalTriangulation", displayId), bufferedImageToPixmap(image));
    }
    
    // --- Вспомогательный метод для конвертации ---
    private Pixmap bufferedImageToPixmap(BufferedImage image) {
        Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                // Конвертируем Java ARGB в arc RGBA8888
                int a = (argb >> 24) & 0xff;
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                pixmap.set(x, y, arc.graphics.Color.rgba8888(r, g, b, a));
            }
        }
        return pixmap;
    }

    // --- Старые методы логирования (один остался) и обработки (без изменений) ---

    private void logEdgeMap(float[][] edgeMap, int displayId) {
        Pixmap edgePixmap = new Pixmap(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = (int)(edgeMap[x][y] * 255);
                edgePixmap.set(x, y, arc.graphics.Color.rgba8888(gray, gray, gray, 255));
            }
        }
        WebLogger.logImage(String.format("slice_%d_1_EdgeMap", displayId), edgePixmap);
        edgePixmap.dispose();
    }

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