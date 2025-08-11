package com.mkso4ka.mindustry.matrixproc;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageProcessor {
    private final Pixmap originalPixmap;
    private boolean[][] used;
    private int width;
    private int height;

    public static class ProcessingSteps {
        public final Pixmap filteredPixmap;
        public final Pixmap quantizedPixmap;
        public final Map<Integer, List<Rect>> result;
        public ProcessingSteps(Pixmap filtered, Pixmap quantized, Map<Integer, List<Rect>> result) {
            this.filteredPixmap = filtered;
            this.quantizedPixmap = quantized;
            this.result = result;
        }
    }

    public ImageProcessor(Pixmap pixmap) {
        this.originalPixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
    }

    public ProcessingSteps process(double tolerance, int filterStrength) {
        Pixmap filteredPixmap = (filterStrength > 0) ? applyMedianFilter(originalPixmap, filterStrength) : originalPixmap;
        // ИСПРАВЛЕНО: Передаем в quantize и отфильтрованное изображение, и допуск
        Pixmap quantizedPixmap = (tolerance > 0) ? quantize(filteredPixmap, tolerance) : filteredPixmap;
        Map<Integer, List<Rect>> rects = groupOptimal(quantizedPixmap);
        return new ProcessingSteps(filteredPixmap, quantizedPixmap, rects);
    }

    private Pixmap applyMedianFilter(Pixmap source, int strength) {
        Pixmap result = new Pixmap(width, height);
        int size = strength * 2 + 1;
        int[] window = new int[size * size];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int count = 0;
                for (int j = -strength; j <= strength; j++) {
                    for (int i = -strength; i <= strength; i++) {
                        int currentX = x + i;
                        int currentY = y + j;
                        if (currentX >= 0 && currentX < width && currentY >= 0 && currentY < height) {
                            window[count++] = source.get(currentX, currentY);
                        }
                    }
                }
                Arrays.sort(window, 0, count);
                result.set(x, y, window[count / 2]);
            }
        }
        WebLogger.info("Median filter applied with strength %d (%dx%d)", strength, size, size);
        return result;
    }

    // ИСПРАВЛЕНО: Метод теперь принимает Pixmap для обработки
    private Pixmap quantize(Pixmap source, double tolerance) {
        Pixmap quantizedPixmap = new Pixmap(width, height);
        List<Integer> palette = new ArrayList<>();
        Map<Integer, double[]> labCache = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // ИСПРАВЛЕНО: Используем 'source', а не 'originalPixmap'
                int originalColor = source.get(x, y);
                
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

    public static Pixmap drawRectsOnPixmap(Pixmap source, Map<Integer, List<Rect>> rects) {
        Pixmap copy = new Pixmap(source.getWidth(), source.getHeight());
        copy.draw(source);
        
        for (List<Rect> rectList : rects.values()) {
            for (Rect rect : rectList) {
                copy.drawRect(rect.x, rect.y, rect.w, rect.h, Color.red.rgba());
            }
        }
        return copy;
    }
}