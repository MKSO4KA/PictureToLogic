package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.struct.StringMap;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import mindustry.world.blocks.logic.LogicDisplay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogicCore {

    private static final int BORDER_SIZE = 8;

    public ProcessingResult processImage(Fi imageFile, int displaysX, int displaysY, LogicDisplay displayBlock, double tolerance, int maxInstructions, int diffusionIterations, float diffusionContrast) {
        try {
            WebLogger.clearDebugImages();
            WebLogger.clearProcessorCodeLogs();

            int displaySize = displayBlock.size;
            int displayPixelSize = getDisplayPixelSize(displaySize);
            int totalWidth = (displaysX * displayPixelSize) + (Math.max(0, displaysX - 1) * BORDER_SIZE * 2);
            int totalHeight = (displaysY * displayPixelSize) + (Math.max(0, displaysY - 1) * BORDER_SIZE * 2);

            Pixmap originalPixmap = new Pixmap(imageFile);

            // --- НАЧАЛО КОСТЫЛЯ: ПОВОРОТ ИЗОБРАЖЕНИЯ НА 90° ВПРАВО ---
            WebLogger.info("Applying 90-degree clockwise rotation crutch...");
            int originalWidth = originalPixmap.getWidth();
            int originalHeight = originalPixmap.getHeight();
            Pixmap masterPixmap = new Pixmap(originalHeight, originalWidth); // Размеры меняются местами

            for (int y = 0; y < originalHeight; y++) {
                for (int x = 0; x < originalWidth; x++) {
                    int color = originalPixmap.get(x, y);
                    // Формула поворота на 90 градусов по часовой стрелке
                    masterPixmap.set(originalHeight - 1 - y, x, color);
                }
            }
            originalPixmap.dispose(); // Освобождаем память от исходного изображения
            // --- КОНЕЦ КОСТЫЛЯ ---

            Pixmap scaledMasterPixmap = new Pixmap(totalWidth, totalHeight);
            scaledMasterPixmap.draw(masterPixmap, 0, 0, masterPixmap.width, masterPixmap.height, 0, 0, totalWidth, totalHeight);
            masterPixmap.dispose(); // Освобождаем память от повернутого изображения

            DisplayMatrix displayMatrix = new DisplayMatrix();
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(displaysX, displaysY, displaySize, DisplayProcessorMatrixFinal.PROCESSOR_REACH);

            int[] processorsPerDisplay = new int[blueprint.displayBottomLefts.length];
            Map<Integer, List<String>> codeMap = new HashMap<>();
            
            List<DisplayCodeInfo> finalCodesForApi = new ArrayList<>();

            for (int i = 0; i < displaysY; i++) {
                for (int j = 0; j < displaysX; j++) {
                    int displayIndex = j * displaysY + i;
                    
                    int sliceWidth = displayPixelSize + (j > 0 ? BORDER_SIZE : 0) + (j < displaysX - 1 ? BORDER_SIZE : 0);
                    int sliceHeight = displayPixelSize + (i > 0 ? BORDER_SIZE : 0) + (i < displaysY - 1 ? BORDER_SIZE : 0);
                    int subX = j * (displayPixelSize + BORDER_SIZE * 2) - (j > 0 ? BORDER_SIZE : 0);
                    int subY = i * (displayPixelSize + BORDER_SIZE * 2) - (i > 0 ? BORDER_SIZE : 0);
                    Pixmap finalSlice = new Pixmap(sliceWidth, sliceHeight);
                    finalSlice.draw(scaledMasterPixmap, subX, subY, sliceWidth, sliceHeight, 0, 0, sliceWidth, sliceHeight);
                    
                    ImageProcessor processor = new ImageProcessor(finalSlice);
                    ImageProcessor.ProcessingSteps steps = processor.process(tolerance, diffusionIterations, diffusionContrast);
                    
                    Map<Integer, List<Rect>> rects = steps.result;
                    int offsetX = (j > 0) ? BORDER_SIZE : 0;
                    int offsetY = (i > 0) ? BORDER_SIZE : 0;
                    List<String> allCommands = generateCommandList(rects, displayPixelSize, offsetX, offsetY);
                    
                    StringBuilder fullCodeBuilder = new StringBuilder();
                    allCommands.forEach(cmd -> fullCodeBuilder.append(cmd).append("\n"));
                    finalCodesForApi.add(new DisplayCodeInfo(displayIndex, fullCodeBuilder.toString(), displayPixelSize));

                    int safeMaxInstructions = maxInstructions - 2;
                    if (safeMaxInstructions < 1) safeMaxInstructions = 1;

                    List<String> finalProcessorCodes = new ArrayList<>();
                    if (!allCommands.isEmpty()) {
                        List<String> currentContent = new ArrayList<>();
                        String activeColor = "";

                        for (String command : allCommands) {
                            if (command.startsWith("draw color")) {
                                activeColor = command;
                                break;
                            }
                        }
                        if (activeColor.isEmpty()) {
                            activeColor = "draw color 0 0 0 255 0 0";
                        }

                        for (String command : allCommands) {
                            if (command.startsWith("draw color")) {
                                if (!currentContent.isEmpty()) {
                                    String chunk = activeColor + "\n" + String.join("\n", currentContent) + "\ndrawflush display1";
                                    finalProcessorCodes.add(chunk);
                                    currentContent.clear();
                                }
                                activeColor = command;
                            } else {
                                currentContent.add(command);
                                if (currentContent.size() >= safeMaxInstructions) {
                                    String chunk = activeColor + "\n" + String.join("\n", currentContent) + "\ndrawflush display1";
                                    finalProcessorCodes.add(chunk);
                                    currentContent.clear();
                                }
                            }
                        }

                        if (!currentContent.isEmpty()) {
                            String chunk = activeColor + "\n" + String.join("\n", currentContent) + "\ndrawflush display1";
                            finalProcessorCodes.add(chunk);
                        }
                    }
                    
                    codeMap.put(displayIndex, finalProcessorCodes);
                    processorsPerDisplay[displayIndex] = finalProcessorCodes.size();

                    finalSlice.dispose();
                }
            }
            scaledMasterPixmap.dispose();

            WebLogger.logDisplayCodes(finalCodesForApi);

            DisplayProcessorMatrixFinal matrixFinal = new DisplayProcessorMatrixFinal(
                blueprint.n, blueprint.m, processorsPerDisplay, blueprint.displayBottomLefts, displaySize
            );
            matrixFinal.placeProcessors();
            
            Schematic schematic = buildSchematic(matrixFinal.getMatrix(), matrixFinal.getDisplays(), codeMap, displayBlock);

            return new ProcessingResult(schematic, matrixFinal.getMatrix(), matrixFinal.getDisplays(), displaySize);

        } catch (Exception e) {
            WebLogger.err("Критическая ошибка в LogicCore!", e);
            return null;
        }
    }

    private Schematic buildSchematic(DisplayProcessorMatrixFinal.Cell[][] matrix, DisplayInfo[] displays, Map<Integer, List<String>> codeMap, Block displayBlock) {
        Seq<Stile> tiles = new Seq<>();
        int height = matrix.length;
        int width = matrix[0].length;
        int processorCount = 0;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                DisplayProcessorMatrixFinal.Cell cell = matrix[row][col];
                if (cell.type == 1 && cell.ownerId >= 0 && cell.processorIndex >= 0) {
                    short schemX = (short)col;
                    short schemY = (short)row;
                    DisplayInfo ownerDisplay = displays[cell.ownerId];
                    
                    String code = "### ERROR: Code not found ###";
                    List<String> codesForDisplay = codeMap.get(cell.ownerId);
                    if (codesForDisplay != null && cell.processorIndex < codesForDisplay.size()) {
                        code = codesForDisplay.get(cell.processorIndex);
                    }

                    String logHeader = String.format("--- Display #%d / Processor #%d ---", cell.ownerId, cell.processorIndex);
                    WebLogger.logProcessorCode(logHeader + "\n" + code);

                    LogicBlock.LogicBuild build = (LogicBlock.LogicBuild) Blocks.microProcessor.newBuilding();
                    build.tile = new Tile(schemX, schemY);
                    int linkToX = ownerDisplay.bottomLeft.x + displayBlock.size / 2;
                    int linkToY = ownerDisplay.bottomLeft.y + displayBlock.size / 2;
                    build.links.add(new LogicLink(linkToX, linkToY, "display1", true));
                    build.updateCode(code);
                    tiles.add(new Stile(Blocks.microProcessor, schemX, schemY, build.config(), (byte) 0));
                    processorCount++;
                }
            }
        }

        for (DisplayInfo display : displays) {
            short finalX = (short)display.bottomLeft.x;
            short finalY = (short)display.bottomLeft.y;
            if (displayBlock.size == 6) { finalX += 2; finalY += 2; }
            else if (displayBlock.size == 3) { finalX += 1; finalY += 1; }
            tiles.add(new Stile(displayBlock, finalX, finalY, null, (byte) 0));
        }
        
        WebLogger.info("[Schematic Stats] Total objects placed: %d (%d displays, %d processors)", displays.length + processorCount, displays.length, processorCount);

        StringMap tags = new StringMap();
        tags.put("name", "PictureToLogic-Schematic");
        return new Schematic(tiles, tags, width, height);
    }

    private List<String> generateCommandList(Map<Integer, List<Rect>> rects, int displayPixelSize, int offsetX, int offsetY) {
        List<String> commands = new ArrayList<>();
        for (Map.Entry<Integer, List<Rect>> entry : rects.entrySet()) {
            List<Rect> rectList = entry.getValue();
            if (!rectList.isEmpty()) {
                commands.add(formatColorCommand(entry.getKey()));
                for (Rect rect : rectList) {
                    int correctedX = rect.x - offsetX;
                    int correctedY = rect.y - offsetY;
                    int mindustryY = displayPixelSize - 1 - correctedY - (rect.h - 1);
                    commands.add(String.format("draw rect %d %d %d %d 0 0", correctedX, mindustryY, rect.w, rect.h));
                }
            }
        }
        return commands;
    }

    private int getDisplayPixelSize(int displayBlockSize) {
        if (displayBlockSize >= 6) return 176;
        return 80;
    }

    private String formatColorCommand(int rgba8888) {
        int r = (rgba8888 >> 24) & 0xff;
        int g = (rgba8888 >> 16) & 0xff;
        int b = (rgba8888 >> 8) & 0xff;
        int a = (rgba8888) & 0xff;
        return String.format("draw color %d %d %d %d 0 0", r, g, b, a);
    }
}
