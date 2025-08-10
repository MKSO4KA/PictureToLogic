package com.mkso4ka.mindustry.matrixproc;

import arc.graphics.Pixmap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageProcessor {
    private final Pixmap originalPixmap;
    private boolean[][] used;
    private int width;
    private int height;

    public ImageProcessor(Pixmap pixmap) {
        this.originalPixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
    }

    /**
     * Главный метод. Сначала квантует цвета, затем ищет прямоугольники.
     * @param tolerance Допуск Delta E. 0 - без изменений, 5-15 - для артов, 20+ для фото.
     * @return Карта с цветами и прямоугольниками.
     */
    public Map<Integer, List<Rect>> process(double tolerance) {
        Pixmap processedPixmap = (tolerance > 0) ? quantize(tolerance) : originalPixmap;
        return groupOptimal(processedPixmap);
    }

    /**
     * Квантует (уменьшает количество) цветов в изображении на основе допуска Delta E.
     */
    private Pixmap quantize(double tolerance) {
        Pixmap quantizedPixmap = new Pixmap(width, height);
        List<Integer> palette = new ArrayList<>();
        Map<Integer, double[]> labCache = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalColor = originalPixmap.get(x, y);
                
                // Прозрачные пиксели не обрабатываем
                if ((originalColor & 0xff) == 0) {
                    quantizedPixmap.set(x, y, originalColor);
                    continue;
                }

                double[] labOriginal = labCache.computeIfAbsent(originalColor, ColorUtils::argbToLab);
                
                Integer bestMatch = null;
                double minDeltaE = Double.MAX_VALUE;

                for (Integer paletteColor : palette) {
                    double[] labPalette = labCache.computeIfAbsent(paletteColor, ColorUtils::argbToLab);
                    double deltaE = ColorUtils.deltaE2000(labOriginal, labPalette);
                    if (deltaE < minDeltaE) {
                        minDeltaE = deltaE;
                        bestMatch = paletteColor;
                    }
                }

                if (bestMatch != null && minDeltaE <= tolerance) {
                    quantizedPixmap.set(x, y, bestMatch);
                } else {
                    palette.add(originalColor);
                    quantizedPixmap.set(x, y, originalColor);
                }
            }
        }
        WebLogger.info("Color quantization complete. Original colors: %d -> Palette size: %d", labCache.size(), palette.size());
        return quantizedPixmap;
    }

    /**
     * Находит оптимальные прямоугольники на (уже обработанном) изображении.
     */
    private Map<Integer, List<Rect>> groupOptimal(Pixmap pixmap) {
        this.used = new boolean[width][height];
        Map<Integer, List<Rect>> out = new HashMap<>();

        while (true) {
            Rect bestRect = null;
            int bestRectArea = -1;
            int bestRectColor = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!used[x][y]) {
                        int currentColor = pixmap.get(x, y);
                        // Прозрачные пиксели не группируем
                        if ((currentColor & 0xff) == 0) continue;

                        Rect candidateRect = findLargestRectangleAt(x, y, currentColor, pixmap);
                        int candidateArea = candidateRect.w * candidateRect.h;

                        if (candidateArea > bestRectArea) {
                            bestRectArea = candidateArea;
                            bestRect = candidateRect;
                            bestRectColor = currentColor;
                        }
                    }
                }
            }

            if (bestRect == null) break;

            out.computeIfAbsent(bestRectColor, k -> new ArrayList<>()).add(bestRect);
            markRect(bestRect);
        }
        return out;
    }

    private Rect findLargestRectangleAt(int startX, int startY, int color, Pixmap pixmap) {
        int maxWidth = 0;
        for (int x = startX; x < width; x++) {
            if (pixmap.get(x, startY) == color && !used[x][startY]) {
                maxWidth++;
            } else {
                break;
            }
        }

        int bestArea = 0;
        Rect bestRect = new Rect(startX, startY, 0, 0);
        for (int y = startY; y < height; y++) {
            int currentWidth = 0;
            for (int x = startX; x < startX + maxWidth; x++) {
                if (pixmap.get(x, y) == color && !used[x][y]) {
                    currentWidth++;
                } else {
                    break;
                }
            }
            maxWidth = Math.min(maxWidth, currentWidth);
            if (maxWidth == 0) break;

            int currentHeight = y - startY + 1;
            int currentArea = maxWidth * currentHeight;
            if (currentArea > bestArea) {
                bestArea = currentArea;
                bestRect = new Rect(startX, startY, maxWidth, currentHeight);
            }
        }
        return bestRect;
    }

    private void markRect(Rect rect) {
        for (int y = 0; y < rect.h; y++) {
            for (int x = 0; x < rect.w; x++) {
                used[rect.x + x][rect.y + y] = true;
            }
        }
    }
}