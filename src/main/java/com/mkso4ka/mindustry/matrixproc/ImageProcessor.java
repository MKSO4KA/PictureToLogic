package com.mkso4ka.mindustry.matrixproc;

import arc.graphics.Pixmap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageProcessor {
    private final Pixmap pixmap;
    private final boolean[][] used;
    private final int width;
    private final int height;

    public ImageProcessor(Pixmap pixmap) {
        this.pixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
        this.used = new boolean[width][height];
    }

    public Map<Integer, List<Rect>> groupOptimal() {
        Map<Integer, List<Rect>> out = new HashMap<>();
        while (true) {
            Rect bestRect = null;
            int bestRectArea = -1;
            int bestRectColor = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!used[x][y]) {
                        int currentColor = pixmap.get(x, y);
                        Rect candidateRect = findLargestRectangleAt(x, y, currentColor);
                        int candidateArea = candidateRect.w * candidateRect.h;
                        if (candidateArea > bestRectArea) {
                            bestRectArea = candidateArea;
                            bestRect = candidateRect;
                            bestRectColor = currentColor;
                        }
                    }
                }
            }
            if (bestRect == null) {
                break;
            }
            out.computeIfAbsent(bestRectColor, k -> new ArrayList<>()).add(bestRect);
            markRect(bestRect);
        }
        return out;
    }

    private Rect findLargestRectangleAt(int startX, int startY, int color) {
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
            if (maxWidth == 0) {
                break;
            }
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