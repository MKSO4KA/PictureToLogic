package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
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

    private static final int COMMANDS_PER_PROCESSOR = 989;
    private static final int BORDER_SIZE = 8;

    public Schematic processImage(Fi imageFile, int displaysX, int displaysY, LogicDisplay displayBlock) {
        try {
            int displaySize = displayBlock.size;
            int displayPixelSize = getDisplayPixelSize(displaySize);
            int totalWidth = (displaysX * displayPixelSize) + (Math.max(0, displaysX - 1) * BORDER_SIZE * 2);
            int totalHeight = (displaysY * displayPixelSize) + (Math.max(0, displaysY - 1) * BORDER_SIZE * 2);

            Pixmap masterPixmap = new Pixmap(imageFile);
            Pixmap scaledMasterPixmap = new Pixmap(totalWidth, totalHeight);
            scaledMasterPixmap.draw(masterPixmap, 0, 0, masterPixmap.width, masterPixmap.height, 0, 0, totalWidth, totalHeight);
            masterPixmap.dispose();

            // 1. Используем ВАШУ логику для создания blueprint
            DisplayMatrix displayMatrix = new DisplayMatrix();
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(displaysX, displaysY, displaySize, DisplayProcessorMatrixFinal.PROCESSOR_REACH);

            int[] processorsPerDisplay = new int[blueprint.displayCoordinates.length];
            Map<Integer, List<String>> codeMap = new HashMap<>();

            for (int i = 0; i < displaysY; i++) {
                for (int j = 0; j < displaysX; j++) {
                    int displayIndex = j * displaysY + i;
                    codeMap.put(displayIndex, new ArrayList<>());
                    // ... (код анализа изображения остается без изменений)
                    int sliceWidth = displayPixelSize + (j > 0 ? BORDER_SIZE : 0) + (j < displaysX - 1 ? BORDER_SIZE : 0);
                    int sliceHeight = displayPixelSize + (i > 0 ? BORDER_SIZE : 0) + (i < displaysY - 1 ? BORDER_SIZE : 0);
                    int subX = j * (displayPixelSize + BORDER_SIZE * 2) - (j > 0 ? BORDER_SIZE : 0);
                    int subY = i * (displayPixelSize + BORDER_SIZE * 2) - (i > 0 ? BORDER_SIZE : 0);
                    Pixmap finalSlice = new Pixmap(sliceWidth, sliceHeight);
                    finalSlice.draw(scaledMasterPixmap, 0, 0, subX, subY, sliceWidth, sliceHeight);
                    ImageProcessor processor = new ImageProcessor(finalSlice);
                    Map<Integer, List<Rect>> rects = processor.groupOptimal();
                    int offsetX = (j > 0) ? BORDER_SIZE : 0;
                    int offsetY = (i > 0) ? BORDER_SIZE : 0;
                    List<String> allCommands = generateCommandList(rects, displayPixelSize, offsetX, offsetY);
                    int commandCount = allCommands.size();
                    processorsPerDisplay[displayIndex] = (int) Math.ceil((double) commandCount / COMMANDS_PER_PROCESSOR);
                    for (int p = 0; p < processorsPerDisplay[displayIndex]; p++) {
                        int start = p * COMMANDS_PER_PROCESSOR;
                        int end = Math.min(start + COMMANDS_PER_PROCESSOR, commandCount);
                        List<String> chunk = allCommands.subList(start, end);
                        StringBuilder codeBuilder = new StringBuilder();
                        chunk.forEach(command -> codeBuilder.append(command).append("\n"));
                        codeBuilder.append("drawflush display1");
                        codeMap.get(displayIndex).add(codeBuilder.toString());
                    }
                    finalSlice.dispose();
                }
            }
            scaledMasterPixmap.dispose();

            // 2. Используем ВАШ алгоритм для размещения процессоров
            DisplayProcessorMatrixFinal matrixFinal = new DisplayProcessorMatrixFinal(
                blueprint.n, blueprint.m, processorsPerDisplay, blueprint.displayCoordinates, displaySize
            );
            matrixFinal.placeProcessors();
            DisplayProcessorMatrixFinal.Cell[][] finalMatrix = matrixFinal.getMatrix();
            DisplayInfo[] finalDisplays = matrixFinal.getDisplays();

            // 3. Собираем чертеж, который будет точной копией вашей матрицы
            return buildSchematic(finalMatrix, finalDisplays, codeMap, displayBlock);

        } catch (Exception e) {
            Log.err("Критическая ошибка в LogicCore!", e);
            return null;
        }
    }

    private Schematic buildSchematic(DisplayProcessorMatrixFinal.Cell[][] matrix, DisplayInfo[] displays, Map<Integer, List<String>> codeMap, Block displayBlock) {
        Seq<Stile> tiles = new Seq<>();
        
        // --- КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ ---
        // Мы должны читать матрицу так же, как она была создана: matrix[x][y]
        int width = matrix.length;
        int height = matrix[0].length;

        for (int x = 0; x < width; x++) { // Внешний цикл по X (столбцы)
            for (int y = 0; y < height; y++) { // Внутренний цикл по Y (строки)
                DisplayProcessorMatrixFinal.Cell cell = matrix[x][y];
                if (cell.type == 0) continue;

                short schemX = (short)x;
                short schemY = (short)y;

                if (cell.type == 2) { // Это клетка дисплея
                    // Мы больше не пытаемся "угадать" центр.
                    // Дисплеи будут поставлены отдельным, гарантированным циклом.
                    // Здесь мы ничего не делаем, чтобы избежать дубликатов.
                } else if (cell.type == 1) { // Это процессор
                    if (cell.ownerId >= 0) {
                        DisplayInfo ownerDisplay = displays[cell.ownerId];
                        
                        String code = "";
                        List<String> codesForDisplay = codeMap.get(cell.ownerId);
                        if (codesForDisplay != null && !codesForDisplay.isEmpty()) {
                            code = codesForDisplay.remove(0);
                        }

                        LogicBlock.LogicBuild build = (LogicBlock.LogicBuild) Blocks.microProcessor.newBuilding();
                        build.tile = new Tile(schemX, schemY);
                        
                        build.links.add(new LogicLink(ownerDisplay.center.x, ownerDisplay.center.y, "display1", true));
                        build.updateCode(code);
                        
                        tiles.add(new Stile(Blocks.microProcessor, schemX, schemY, build.config(), (byte) 0));
                    }
                }
            }
        }

        // ГАРАНТИРОВАННОЕ РАЗМЕЩЕНИЕ ДИСПЛЕЕВ
        // Мы проходимся по исходному списку и ставим ровно столько дисплеев, сколько нужно.
        for (DisplayInfo display : displays) {
            tiles.add(new Stile(displayBlock, (short)display.center.x, (short)display.center.y, null, (byte) 0));
        }
        
        StringMap tags = new StringMap();
        tags.put("name", "PictureToLogic-Schematic");
        return new Schematic(tiles, tags, width, height);
    }
    
    // ... (остальные вспомогательные методы остаются без изменений) ...
    private List<String> generateCommandList(Map<Integer, List<Rect>> rects, int displayPixelSize, int offsetX, int offsetY) {
        List<String> commands = new ArrayList<>();
        for (Map.Entry<Integer, List<Rect>> entry : rects.entrySet()) {
            List<Rect> rectList = entry.getValue();
            if (!rectList.isEmpty()) {
                commands.add(formatColorCommand(entry.getKey()));
                for (Rect rect : rectList) {
                    int correctedX = rect.x - offsetX;
                    int correctedY = rect.y - offsetY;
                    int mindustryY = displayPixelSize - correctedY - rect.h;
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