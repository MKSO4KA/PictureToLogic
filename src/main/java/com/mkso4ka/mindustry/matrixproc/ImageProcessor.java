package com.mkso4ka.mindustry.matrixproc;

import arc.graphics.Color;
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

    public ProcessingSteps process(double tolerance, int diffusionIterations, float diffusionContrast) {
        Pixmap filteredPixmap = (diffusionIterations > 0) ? applyAnisotropicDiffusion(originalPixmap, diffusionIterations, diffusionContrast) : originalPixmap;
        Pixmap quantizedPixmap = (tolerance > 0) ? quantize(filteredPixmap, tolerance) : filteredPixmap;
        Map<Integer, List<Rect>> rects = groupOptimal(quantizedPixmap);
        return new ProcessingSteps(filteredPixmap, quantizedPixmap, rects);
    }

    private Pixmap applyAnisotropicDiffusion(Pixmap source, int iterations, float k) {
        Pixmap current = new Pixmap(width, height);
        current.draw(source);
        Pixmap next = new Pixmap(width, height);

        for (int i = 0; i < iterations; i++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int c = current.get(x, y);
                    float r = (c >> 24 & 0xff);
                    float g = (c >> 16 & 0xff);
                    float b = (c >> 8 & 0xff);

                    float totalR = 0, totalG = 0, totalB = 0;
                    float totalWeight = 0;

                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                int nc = current.get(nx, ny);
                                int nr = (nc >> 24 & 0xff);
                                int ng = (nc >> 16 & 0xff);
                                int nb = (nc >> 8 & 0xff);

                                float gradient = Math.abs((r + g + b) - (nr + ng + nb)) / 3f;
                                float weight = (float) Math.exp(-Math.pow(gradient / k, 2));

                                totalR += nr * weight;
                                totalG += ng * weight;
                                totalB += nb * weight;
                                totalWeight += weight;
                            }
                        }
                    }
                    int finalR = (int) (totalR / totalWeight);
                    int finalG = (int) (totalG / totalWeight);
                    int finalB = (int) (totalB / totalWeight);
                    next.set(x, y, (finalR << 24) | (finalG << 16) | (finalB << 8) | (c & 0xff));
                }
            }
            current.draw(next);
        }
        WebLogger.info("Anisotropic diffusion applied. Iterations: %d, K: %.1f", iterations, k);
        return current;
    }

    private Pixmap quantize(Pixmap source, double tolerance) {
        Pixmap quantizedPixmap = new Pixmap(width, height);
        List<Integer> palette = new ArrayList<>();
        Map<Integer, double[]> labCache = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
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
        WebLogger.info("Color quantization complete. Palette size: %d", palette.size());
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
