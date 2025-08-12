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

    public ProcessingResult processImage(Fi imageFile, int displaysX, int displaysY, LogicDisplay displayBlock, double detail, int maxInstructions, boolean useTransparentBg) {
        try {
            WebLogger.clearDebugImages();
            WebLogger.clearProcessorCodeLogs();

            Pixmap masterPixmap = new Pixmap(imageFile);

            int displaySize = displayBlock.size;
            int displayPixelSize = getDisplayPixelSize(displaySize);
            
            int totalDisplayWidth = displaysX * displayPixelSize;
            int totalDisplayHeight = displaysY * displayPixelSize;

            Pixmap scaledMasterPixmap = new Pixmap(totalDisplayWidth, totalDisplayHeight);
            scaledMasterPixmap.draw(masterPixmap, 0, 0, masterPixmap.width, masterPixmap.height, 0, 0, totalDisplayWidth, totalDisplayHeight);
            masterPixmap.dispose();

            DisplayMatrix displayMatrix = new DisplayMatrix();
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(displaysX, displaysY, displaySize, DisplayProcessorMatrixFinal.PROCESSOR_REACH);

            int[] processorsPerDisplay = new int[blueprint.displayBottomLefts.length];
            Map<Integer, List<String>> codeMap = new HashMap<>();
            
            List<DisplayCodeInfo> finalCodesForApi = new ArrayList<>();

            for (int i = 0; i < displaysY; i++) {
                for (int j = 0; j < displaysX; j++) {
                    int displayIndex = j * displaysY + i;
                    
                    int displayStartX = j * displayPixelSize;
                    int displayStartY = i * displayPixelSize;

                    int sliceStartX = Math.max(0, displayStartX - BORDER_SIZE);
                    int sliceStartY = Math.max(0, displayStartY - BORDER_SIZE);
                    int sliceEndX = Math.min(totalDisplayWidth, displayStartX + displayPixelSize + BORDER_SIZE);
                    int sliceEndY = Math.min(totalDisplayHeight, displayStartY + displayPixelSize + BORDER_SIZE);

                    int sliceWidth = sliceEndX - sliceStartX;
                    int sliceHeight = sliceEndY - sliceStartY;

                    Pixmap finalSlice = new Pixmap(sliceWidth, sliceHeight);
                    finalSlice.draw(scaledMasterPixmap, sliceStartX, sliceStartY, sliceWidth, sliceHeight, 0, 0, sliceWidth, sliceHeight);
                    
                    if (useTransparentBg) {
                        int bgColor = finalSlice.get(0, 0);
                        for (int y = 0; y < finalSlice.getHeight(); y++) {
                            for (int x = 0; x < finalSlice.getWidth(); x++) {
                                if (finalSlice.get(x, y) == bgColor) {
                                    finalSlice.set(x, y, 0);
                                }
                            }
                        }
                    }

                    ImageProcessor processor = new ImageProcessor(finalSlice);
                    ImageProcessor.ProcessingSteps steps = processor.process(detail, displayIndex);
                    
                    List<String> allCommands = generateTriangleCommandList(
                        steps.result, 
                        displayPixelSize, 
                        sliceStartX, 
                        sliceStartY, 
                        displayStartX, 
                        displayStartY
                    );
                    
                    StringBuilder fullCodeBuilder = new StringBuilder();
                    allCommands.forEach(cmd -> fullCodeBuilder.append(cmd).append("\n"));
                    finalCodesForApi.add(new DisplayCodeInfo(displayIndex, fullCodeBuilder.toString(), displayPixelSize));

                    // --- НАЧАЛО ИСПРАВЛЕННОГО БЛОКА ---

                    int safeMaxInstructions = maxInstructions - 1;
                    if (safeMaxInstructions < 1) safeMaxInstructions = 1;

                    List<String> finalProcessorCodes = new ArrayList<>();
                    if (!allCommands.isEmpty()) {
                        List<String> currentChunk = new ArrayList<>();
                        String lastSeenColor = ""; 

                        for (String command : allCommands) {
                            if (currentChunk.isEmpty()) {
                                if (!command.startsWith("draw color")) {
                                    if (!lastSeenColor.isEmpty()) {
                                        currentChunk.add(lastSeenColor);
                                    }
                                }
                            }

                            currentChunk.add(command);

                            if (command.startsWith("draw color")) {
                                lastSeenColor = command;
                            }

                            if (currentChunk.size() >= safeMaxInstructions) {
                                if (currentChunk.get(currentChunk.size() - 1).startsWith("draw color")) {
                                    currentChunk.remove(currentChunk.size() - 1);
                                }

                                currentChunk.add("drawflush display1");
                                finalProcessorCodes.add(String.join("\n", currentChunk));
                                currentChunk.clear();
                            }
                        }

                        if (!currentChunk.isEmpty()) {
                            currentChunk.add("drawflush display1");
                            finalProcessorCodes.add(String.join("\n", currentChunk));
                        }
                    }
                    
                    // --- КОНЕЦ ИСПРАВЛЕННОГО БЛОКА ---
                    
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

    private List<String> generateTriangleCommandList(
        Map<Integer, List<ImageProcessor.Triangle>> triangles, 
        int displayPixelSize, 
        int sliceStartX, 
        int sliceStartY, 
        int displayStartX, 
        int displayStartY
    ) {
        List<String> commands = new ArrayList<>();
        int maxCoord = displayPixelSize - 1;

        for (Map.Entry<Integer, List<ImageProcessor.Triangle>> entry : triangles.entrySet()) {
            List<ImageProcessor.Triangle> triangleList = entry.getValue();
            if (!triangleList.isEmpty()) {
                commands.add(formatColorCommand(entry.getKey()));
                for (ImageProcessor.Triangle t : triangleList) {
                    int globalX1 = sliceStartX + t.x1;
                    int globalY1 = sliceStartY + t.y1;
                    int globalX2 = sliceStartX + t.x2;
                    int globalY2 = sliceStartY + t.y2;
                    int globalX3 = sliceStartX + t.x3;
                    int globalY3 = sliceStartY + t.y3;

                    int localX1 = globalX1 - displayStartX;
                    int localY1 = globalY1 - displayStartY;
                    int localX2 = globalX2 - displayStartX;
                    int localY2 = globalY2 - displayStartY;
                    int localX3 = globalX3 - displayStartX;
                    int localY3 = globalY3 - displayStartY;

                    int x1 = Math.max(0, Math.min(maxCoord, localX1));
                    int y1 = maxCoord - Math.max(0, Math.min(maxCoord, localY1));
                    int x2 = Math.max(0, Math.min(maxCoord, localX2));
                    int y2 = maxCoord - Math.max(0, Math.min(maxCoord, localY2));
                    int x3 = Math.max(0, Math.min(maxCoord, localX3));
                    int y3 = maxCoord - Math.max(0, Math.min(maxCoord, localY3));

                    commands.add(String.format("draw triangle %d %d %d %d %d %d 0 0", x1, y1, x2, y2, x3, y3));
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