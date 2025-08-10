package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.gen.LogicIO;
import mindustry.logic.LogicIO.LogicLink;
import mindustry.world.Block;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.MessageBlock;
import mindustry.world.blocks.logic.LogicDisplay;
import mindustry.content.Blocks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LogicCore {

    private static final int COMMANDS_PER_PROCESSOR = 989;
    private static final int BORDER_SIZE = 8;

    /**
     * Главный метод, который запускает всю цепочку обработки.
     * @return Готовый объект Schematic или null в случае ошибки.
     */
    public Schematic processImage(Fi imageFile, int displaysX, int displaysY) {
        try {
            Log.info("--- НАЧАЛО ОБРАБОТКИ ---");
            int displaySize = 3;
            Block displayBlock = Blocks.largeLogicDisplay; // Используем большой дисплей

            // 1. Анализ изображения и расчет потребностей
            int displayPixelSize = getDisplayPixelSize(displaySize);
            int totalWidth = (displaysX * displayPixelSize) + (Math.max(0, displaysX - 1) * BORDER_SIZE * 2);
            int totalHeight = (displaysY * displayPixelSize) + (Math.max(0, displaysY - 1) * BORDER_SIZE * 2);

            Pixmap masterPixmap = new Pixmap(imageFile);
            Pixmap scaledMasterPixmap = new Pixmap(totalWidth, totalHeight);
            scaledMasterPixmap.draw(masterPixmap, 0, 0, masterPixmap.width, masterPixmap.height, 0, 0, totalWidth, totalHeight);
            masterPixmap.dispose();

            DisplayMatrix displayMatrix = new DisplayMatrix();
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(displaysX, displaysY, displaySize, DisplayProcessorMatrixFinal.PROCESSOR_REACH);

            int[] processorsPerDisplay = new int[blueprint.displayCoordinates.length];
            List<List<String>> allProcessorsCode = new ArrayList<>();

            for (int i = 0; i < displaysY; i++) {
                for (int j = 0; j < displaysX; j++) {
                    int displayIndex = j * displaysY + i;

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

                    // Сохраняем код для каждого процессора
                    for (int p = 0; p < processorsPerDisplay[displayIndex]; p++) {
                        int start = p * COMMANDS_PER_PROCESSOR;
                        int end = Math.min(start + COMMANDS_PER_PROCESSOR, commandCount);
                        List<String> chunk = allCommands.subList(start, end);
                        StringBuilder codeBuilder = new StringBuilder();
                        chunk.forEach(command -> codeBuilder.append(command).append("\n"));
                        codeBuilder.append("drawflush display1");
                        allProcessorsCode.add(Arrays.asList(String.valueOf(displayIndex), codeBuilder.toString()));
                    }
                    finalSlice.dispose();
                }
            }
            scaledMasterPixmap.dispose();

            // 2. Размещение блоков
            DisplayProcessorMatrixFinal matrixFinal = new DisplayProcessorMatrixFinal(
                blueprint.n, blueprint.m, processorsPerDisplay, blueprint.displayCoordinates, displaySize
            );
            matrixFinal.placeProcessors();
            Cell[][] finalMatrix = matrixFinal.getMatrix();
            DisplayInfo[] finalDisplays = matrixFinal.getDisplays();

            // 3. Создание объекта Schematic
            return buildSchematic(finalMatrix, finalDisplays, allProcessorsCode, displayBlock);

        } catch (Exception e) {
            Log.err("Критическая ошибка в LogicCore!", e);
            return null; // Возвращаем null в случае ошибки
        }
    }

    private Schematic buildSchematic(Cell[][] matrix, DisplayInfo[] displays, List<List<String>> allProcessorsCode, Block displayBlock) {
        Seq<Stile> tiles = new Seq<>();
        int processorCodeIndex = 0;

        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                Cell cell = matrix[x][y];
                if (cell.type == 0) continue; // Пропускаем пустые клетки

                if (cell.type == 2) { // Это дисплей
                    // Находим центр группы дисплеев
                    if (isCenterOfBlock(x, y, cell.ownerId, matrix, 3)) {
                        tiles.add(new Stile(displayBlock, x, y, null, (byte) 0));
                    }
                } else if (cell.type == 1) { // Это процессор
                    if (cell.ownerId >= 0) {
                        DisplayInfo ownerDisplay = displays[cell.ownerId];
                        
                        // Ищем код для этого процессора
                        String code = "";
                        for(List<String> codeEntry : allProcessorsCode) {
                            if(Integer.parseInt(codeEntry.get(0)) == cell.ownerId) {
                                code = codeEntry.get(1);
                                allProcessorsCode.remove(codeEntry); // Удаляем, чтобы не использовать повторно
                                break;
                            }
                        }

                        // Создаем линк к дисплею
                        LogicLink[] links = {new LogicLink(ownerDisplay.center.x, ownerDisplay.center.y, "display1", true)};
                        byte[] config = LogicIO.write(code, links);
                        
                        tiles.add(new Stile(Blocks.microProcessor, x, y, config, (byte) 0));
                    }
                }
            }
        }
        
        StringMap tags = new StringMap();
        tags.put("name", "PictureToLogic-Schematic");
        return new Schematic(tiles, tags, matrix.length, matrix[0].length);
    }
    
    // Вспомогательный метод, чтобы ставить дисплей только один раз
    private boolean isCenterOfBlock(int x, int y, int ownerId, Cell[][] matrix, int size) {
        int offset = size / 2;
        return matrix[x-offset][y-offset].ownerId == ownerId && matrix[x+offset][y+offset].ownerId == ownerId;
    }

    // Остальные вспомогательные методы без изменений
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
        return (displayBlockSize == 3) ? 80 : 176;
    }

    private String formatColorCommand(int rgba8888) {
        int r = (rgba8888 >> 24) & 0xff;
        int g = (rgba8888 >> 16) & 0xff;
        int b = (rgba8888 >> 8) & 0xff;
        int a = (rgba8888) & 0xff;
        return String.format("draw color %d %d %d %d 0 0", r, g, b, a);
    }
}