package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
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
import org.waveware.delaunator.DPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogicCore {
    private static final int BORDER_SIZE = 8;

    private static class SliceProcessingResult {
        final int displayIndex;
        final List<String> processorCodes;
        SliceProcessingResult(int displayIndex, List<String> codes) {
            this.displayIndex = displayIndex;
            this.processorCodes = codes;
        }
    }

    public ProcessingResult processImage(Fi imageFile, int displaysX, int displaysY, LogicDisplay displayBlock, double detail, int maxInstructions, boolean useTransparentBg, final AtomicBoolean cancellationToken) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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

            List<Future<SliceProcessingResult>> futures = new ArrayList<>();

            for (int i = 0; i < displaysY; i++) {
                for (int j = 0; j < displaysX; j++) {
                    if (cancellationToken.get()) {
                        WebLogger.warn("Processing cancelled by user before starting all tasks.");
                        return null;
                    }
                    final int displayIndex = j * displaysY + i;
                    final int currentI = i;
                    final int currentJ = j;
                    Callable<SliceProcessingResult> task = () -> {
                        int displayStartX = currentJ * displayPixelSize;
                        int displayStartY = currentI * displayPixelSize;
                        int sliceStartX = Math.max(0, displayStartX - BORDER_SIZE);
                        int sliceStartY = Math.max(0, displayStartY - BORDER_SIZE);
                        int sliceEndX = Math.min(totalDisplayWidth, displayStartX + displayPixelSize + BORDER_SIZE);
                        int sliceEndY = Math.min(totalDisplayHeight, displayStartY + displayPixelSize + BORDER_SIZE);
                        int sliceWidth = sliceEndX - sliceStartX;
                        int sliceHeight = sliceEndY - sliceStartY;
                        Pixmap finalSlice = new Pixmap(sliceWidth, sliceHeight);
                        synchronized (scaledMasterPixmap) {
                            finalSlice.draw(scaledMasterPixmap, sliceStartX, sliceStartY, sliceWidth, sliceHeight, 0, 0, sliceWidth, sliceHeight);
                        }
                        if (useTransparentBg) {
                            int bgColor = finalSlice.get(0, 0);
                            for (int y = 0; y < finalSlice.getHeight(); y++) {
                                for (int x = 0; x < finalSlice.getWidth(); x++) {
                                    if (finalSlice.get(x, y) == bgColor) finalSlice.set(x, y, 0);
                                }
                            }
                        }
                        ImageProcessor imageProc = new ImageProcessor(finalSlice);
                        ImageProcessor.ProcessingSteps steps = imageProc.process(detail, displayIndex);
                        finalSlice.dispose();
                        List<String> allCommands = generateTriangleCommandList(steps.result, displayPixelSize, sliceStartX, sliceStartY, displayStartX, displayStartY);
                        List<String> finalProcessorCodes = splitCommandsIntoChunks(allCommands, maxInstructions);
                        return new SliceProcessingResult(displayIndex, finalProcessorCodes);
                    };
                    futures.add(executor.submit(task));
                }
            }

            Map<Integer, List<String>> codeMap = new ConcurrentHashMap<>();
            int[] processorsPerDisplay = new int[blueprint.displayBottomLefts.length];

            for (Future<SliceProcessingResult> future : futures) {
                if (cancellationToken.get()) {
                    WebLogger.warn("Processing cancelled by user while collecting results.");
                    futures.forEach(f -> f.cancel(true));
                    return null;
                }
                SliceProcessingResult result = future.get();
                codeMap.put(result.displayIndex, result.processorCodes);
                processorsPerDisplay[result.displayIndex] = result.processorCodes.size();
            }
            
            scaledMasterPixmap.dispose();

            DisplayProcessorMatrixFinal matrixFinal = new DisplayProcessorMatrixFinal(blueprint.n, blueprint.m, processorsPerDisplay, blueprint.displayBottomLefts, displaySize);
            matrixFinal.placeProcessors();
            
            Schematic schematic = buildSchematic(matrixFinal.getMatrix(), matrixFinal.getDisplays(), codeMap, displayBlock);
            return new ProcessingResult(schematic, matrixFinal.getMatrix(), matrixFinal.getDisplays(), displaySize);

        } catch (Exception e) {
            if (e instanceof InterruptedException || e instanceof CancellationException) {
                WebLogger.warn("Processing thread was interrupted or cancelled.");
            } else {
                WebLogger.err("Критическая ошибка в LogicCore!", e);
            }
            return null;
        } finally {
            executor.shutdownNow();
        }
    }
    
    private List<String> splitCommandsIntoChunks(List<String> allCommands, int maxInstructions) {
        int safeMaxInstructions = maxInstructions - 1;
        if (safeMaxInstructions < 1) safeMaxInstructions = 1;
        List<String> finalProcessorCodes = new ArrayList<>();
        if (!allCommands.isEmpty()) {
            List<String> currentChunk = new ArrayList<>();
            String lastSeenColor = ""; 
            for (String command : allCommands) {
                if (currentChunk.isEmpty()) {
                    if (!command.startsWith("draw color") && !lastSeenColor.isEmpty()) {
                        currentChunk.add(lastSeenColor);
                    }
                }
                currentChunk.add(command);
                if (command.startsWith("draw color")) lastSeenColor = command;
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
        return finalProcessorCodes;
    }

    private Schematic buildSchematic(DisplayProcessorMatrixFinal.Cell[][] matrix, DPoint[] displays, Map<Integer, List<String>> codeMap, Block displayBlock) {
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
                    DPoint ownerDisplay = displays[cell.ownerId];
                    
                    String code = "### ERROR: Code not found ###";
                    List<String> codesForDisplay = codeMap.get(cell.ownerId);
                    if (codesForDisplay != null && cell.processorIndex < codesForDisplay.size()) {
                        code = codesForDisplay.get(cell.processorIndex);
                    }

                    String logHeader = String.format("--- Display #%d / Processor #%d ---", cell.ownerId, cell.processorIndex);
                    WebLogger.logProcessorCode(logHeader + "\n" + code);

                    LogicBlock.LogicBuild build = (LogicBlock.LogicBuild) Blocks.microProcessor.newBuilding();
                    build.tile = new Tile(schemX, schemY);

                    int displayMinX = (int)ownerDisplay.x;
                    int displayMinY = (int)ownerDisplay.y;
                    int displayMaxX = (int)ownerDisplay.x + displayBlock.size - 1;
                    int displayMaxY = (int)ownerDisplay.y + displayBlock.size - 1;
                    int linkToX = Math.max(displayMinX, Math.min(schemX, displayMaxX));
                    int linkToY = Math.max(displayMinY, Math.min(schemY, displayMaxY));
                    build.links.add(new LogicLink(linkToX, linkToY, "display1", true));
                    
                    build.updateCode(code);
                    tiles.add(new Stile(Blocks.microProcessor, schemX, schemY, build.config(), (byte) 0));
                    processorCount++;
                }
            }
        }

        for (DPoint display : displays) {
            short finalX = (short)display.x;
            short finalY = (short)display.y;
            if (displayBlock.size == 6) { finalX += 2; finalY += 2; }
            else if (displayBlock.size == 3) { finalX += 1; finalY += 1; }
            tiles.add(new Stile(displayBlock, finalX, finalY, null, (byte) 0));
        }
        
        WebLogger.info("[Schematic Stats] Total objects placed: %d (%d displays, %d processors)", displays.length + processorCount, displays.length, processorCount);

        StringMap tags = new StringMap();
        tags.put("name", "PictureToLogic-Schematic");
        return new Schematic(tiles, tags, width, height);
    }

    private List<String> generateTriangleCommandList(Map<Integer, List<ImageProcessor.Triangle>> triangles, int displayPixelSize, int sliceStartX, int sliceStartY, int displayStartX, int displayStartY) {
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
