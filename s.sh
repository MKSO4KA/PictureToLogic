# ==============================================================================
# === Финальный скрипт для исправления сборки PictureToLogic ===
# ==============================================================================

# --- 1. Восстанавливаем правильный DisplayInfo.java из вашего репозитория ---
cat << 'EOF' > src/main/java/com/mkso4ka/mindustry/matrixproc/DisplayInfo.java
package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2;

/**
 * Хранит информацию о конкретном дисплее в схеме.
 */
class DisplayInfo {
    final int id;
    // Храним нижний левый угол, а не центр
    final Point2 bottomLeft;
    final int totalProcessorsRequired;
    int processorsPlaced = 0;

    DisplayInfo(int id, Point2 bottomLeft, int required) {
        this.id = id;
        this.bottomLeft = bottomLeft;
        this.totalProcessorsRequired = required;
    }

    public int getProcessorsNeeded() {
        return totalProcessorsRequired - processorsPlaced;
    }
}
EOF

# --- 2. Исправляем DisplayProcessorMatrixFinal.java (добавляем public и исправляем типы) ---
cat << 'EOF' > src/main/java/com/mkso4ka/mindustry/matrixproc/DisplayProcessorMatrixFinal.java
package com.mkso4ka.mindustry.matrixproc;

import arc.math.geom.Point2;
import arc.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class DisplayProcessorMatrixFinal {

    private static class Spot {
        final Point2 location;
        final double distanceSq;
        Spot(Point2 location, double distanceSq) { this.location = location; this.distanceSq = distanceSq; }
    }

    static class Cell {
        int type = 0;
        int ownerId = -1;
        int processorIndex = -1;
    }

    public static final double PROCESSOR_REACH = 10.2;
    private static final double PROCESSOR_REACH_SQ = PROCESSOR_REACH * PROCESSOR_REACH;

    private final int n, m, displaySize;
    private final Cell[][] matrix;
    private final DisplayInfo[] displays;

    public DisplayProcessorMatrixFinal(int n, int m, int[] processorsPerDisplay, int[][] displayBottomLefts, int displaySize) {
        this.n = n;
        this.m = m;
        this.displaySize = displaySize;
        this.matrix = new Cell[n][m];
        for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) matrix[i][j] = new Cell();
        
        this.displays = new DisplayInfo[displayBottomLefts.length];
        for (int i = 0; i < displayBottomLefts.length; i++) {
            Point2 bottomLeft = new Point2(displayBottomLefts[i][0], displayBottomLefts[i][1]);
            // ИСПРАВЛЕНИЕ: Конструктор теперь соответствует DisplayInfo
            displays[i] = new DisplayInfo(i, bottomLeft, processorsPerDisplay[i]);
            placeSingleDisplay(displays[i]);
        }
    }

    public Cell[][] getMatrix() { return this.matrix; }
    public DisplayInfo[] getDisplays() { return this.displays; }

    private void placeSingleDisplay(DisplayInfo display) {
        for (int i = 0; i < displaySize; i++) {
            for (int j = 0; j < displaySize; j++) {
                int currentX = display.bottomLeft.x + j;
                int currentY = display.bottomLeft.y + i;
                if (currentX >= 0 && currentX < m && currentY >= 0 && currentY < n) {
                    matrix[currentY][currentX].type = 2;
                    matrix[currentY][currentX].ownerId = display.id;
                }
            }
        }
    }

    public void placeProcessors() {
        WebLogger.info("Запуск стратегического алгоритма с приоритетом по нужде...");
        List<Point2> allPossibleSpots = new ArrayList<>();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                if (matrix[y][x].type == 0 && isWithinProcessorReachOfAnyDisplay(new Point2(x, y))) {
                    allPossibleSpots.add(new Point2(x, y));
                }
            }
        }
        WebLogger.info("Найдено " + allPossibleSpots.size() + " возможных мест для процессоров.");

        boolean[][] isSpotTaken = new boolean[n][m];
        List<DisplayInfo> sortedDisplays = new ArrayList<>();
        for (DisplayInfo d : displays) sortedDisplays.add(d);
        sortedDisplays.sort(Comparator.comparingInt(DisplayInfo::getProcessorsNeeded).reversed());

        for (DisplayInfo display : sortedDisplays) {
            int needed = display.getProcessorsNeeded();
            if (needed == 0) continue;

            List<Spot> potentialSpots = new ArrayList<>();
            for (Point2 spotLoc : allPossibleSpots) {
                if (!isSpotTaken[spotLoc.y][spotLoc.x]) {
                    double distSq = distanceSqFromPointToRectangle(spotLoc, display);
                    if (distSq <= PROCESSOR_REACH_SQ) {
                        potentialSpots.add(new Spot(spotLoc, distSq));
                    }
                }
            }
            potentialSpots.sort(Comparator.comparingDouble(s -> s.distanceSq));

            int placedCount = 0;
            for (Spot spot : potentialSpots) {
                if (placedCount >= needed) break;
                Point2 loc = spot.location;
                if (!isSpotTaken[loc.y][loc.x]) {
                    matrix[loc.y][loc.x].type = 1;
                    matrix[loc.y][loc.x].ownerId = display.id;
                    matrix[loc.y][loc.x].processorIndex = display.processorsPlaced;
                    display.processorsPlaced++;
                    isSpotTaken[loc.y][loc.x] = true;
                    placedCount++;
                }
            }
            if (placedCount < needed) {
                WebLogger.warn("   -> ВНИМАНИЕ: Дисплей " + display.id + " получил только " + placedCount + " из " + needed + " процессоров. Не хватило физического места.");
            }
        }
        WebLogger.info("Размещение требуемых процессоров завершено.");
    }

    // ИСПРАВЛЕНИЕ: Делаем метод public для доступа из ModUI
    public boolean isWithinProcessorReachOfAnyDisplay(Point2 p) {
        for (DisplayInfo display : displays) {
            if (distanceSqFromPointToRectangle(p, display) <= PROCESSOR_REACH_SQ) {
                return true;
            }
        }
        return false;
    }

    private double distanceSqFromPointToRectangle(Point2 p, DisplayInfo display) {
        int minX = display.bottomLeft.x;
        int maxX = display.bottomLeft.x + displaySize - 1;
        int minY = display.bottomLeft.y;
        int maxY = display.bottomLeft.y + displaySize - 1;
        
        double closestX = Math.max(minX, Math.min(p.x, maxX));
        double closestY = Math.max(minY, Math.min(p.y, maxY));
        
        double dx = p.x - closestX;
        double dy = p.y - closestY;
        
        return dx * dx + dy * dy;
    }
}
EOF

# --- 3. Исправляем ProcessingResult.java (поля и конструктор) ---
cat << 'EOF' > src/main/java/com/mkso4ka/mindustry/matrixproc/ProcessingResult.java
package com.mkso4ka.mindustry.matrixproc;

import mindustry.game.Schematic;

public class ProcessingResult {
    public final Schematic schematic;
    public final DisplayProcessorMatrixFinal.Cell[][] matrix;
    // ИСПРАВЛЕНИЕ: Тип поля теперь DisplayInfo[]
    public final DisplayInfo[] displays;
    public final int displaySize;
    // ИСПРАВЛЕНИЕ: Добавляем недостающие поля
    public final int matrixWidth;
    public final int matrixHeight;

    // ИСПРАВЛЕНИЕ: Конструктор принимает DisplayInfo[]
    public ProcessingResult(Schematic schematic, DisplayProcessorMatrixFinal.Cell[][] matrix, DisplayInfo[] displays, int displaySize) {
        this.schematic = schematic;
        this.matrix = matrix;
        this.displays = displays;
        this.displaySize = displaySize;
        this.matrixHeight = matrix.length;
        this.matrixWidth = matrix[0].length;
    }
}
EOF

# --- 4. Исправляем LogicCore.java (интеграция и исправление ошибок) ---
cat << 'EOF' > src/main/java/com/mkso4ka/mindustry/matrixproc/LogicCore.java
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

    public ProcessingResult processImage(Fi imageFile, int displaysX, int displaysY, LogicDisplay displayBlock, double tolerance, int maxInstructions, int diffusionIterations, float diffusionContrast, boolean useTransparentBg, final AtomicBoolean cancellationToken) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            WebLogger.clearDebugImages();
            WebLogger.clearProcessorCodeLogs();

            Pixmap masterPixmap = new Pixmap(imageFile);
            int displaySize = displayBlock.size;
            int displayPixelSize = getDisplayPixelSize(displaySize);
            int totalWidth = (displaysX * displayPixelSize) + (Math.max(0, displaysX - 1) * BORDER_SIZE * 2);
            int totalHeight = (displaysY * displayPixelSize) + (Math.max(0, displaysY - 1) * BORDER_SIZE * 2);

            Pixmap scaledMasterPixmap = new Pixmap(totalWidth, totalHeight);
            scaledMasterPixmap.draw(masterPixmap, 0, 0, masterPixmap.width, masterPixmap.height, 0, 0, totalWidth, totalHeight);
            masterPixmap.dispose();

            DisplayMatrix displayMatrix = new DisplayMatrix();
            // ИСПРАВЛЕНИЕ: Передаем int в placeDisplaysXxY
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(displaysX, displaysY, displaySize, (int)DisplayProcessorMatrixFinal.PROCESSOR_REACH);

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
                        int sliceWidth = displayPixelSize + (currentJ > 0 ? BORDER_SIZE : 0) + (currentJ < displaysX - 1 ? BORDER_SIZE : 0);
                        int sliceHeight = displayPixelSize + (currentI > 0 ? BORDER_SIZE : 0) + (currentI < displaysY - 1 ? BORDER_SIZE : 0);
                        int subX = currentJ * (displayPixelSize + BORDER_SIZE * 2) - (currentJ > 0 ? BORDER_SIZE : 0);
                        int subY = currentI * (displayPixelSize + BORDER_SIZE * 2) - (currentI > 0 ? BORDER_SIZE : 0);
                        
                        Pixmap finalSlice = new Pixmap(sliceWidth, sliceHeight);
                        synchronized (scaledMasterPixmap) {
                            finalSlice.draw(scaledMasterPixmap, subX, subY, sliceWidth, sliceHeight, 0, 0, sliceWidth, sliceHeight);
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
                        ImageProcessor.ProcessingSteps steps = imageProc.process(tolerance, diffusionIterations, diffusionContrast);
                        finalSlice.dispose();

                        int offsetX = (currentJ > 0) ? BORDER_SIZE : 0;
                        int offsetY = (currentI > 0) ? BORDER_SIZE : 0;
                        List<String> allCommands = generateTriangleCommandList(steps.result, displayPixelSize, offsetX, offsetY);
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
                    
                    int displayMinX = ownerDisplay.bottomLeft.x;
                    int displayMinY = ownerDisplay.bottomLeft.y;
                    int displayMaxX = ownerDisplay.bottomLeft.x + displayBlock.size - 1;
                    int displayMaxY = ownerDisplay.bottomLeft.y + displayBlock.size - 1;
                    int linkToX = Math.max(displayMinX, Math.min(schemX, displayMaxX));
                    int linkToY = Math.max(displayMinY, Math.min(schemY, displayMaxY));
                    
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

    private List<String> generateTriangleCommandList(Map<Integer, List<ImageProcessor.Triangle>> triangles, int displayPixelSize, int offsetX, int offsetY) {
        List<String> commands = new ArrayList<>();
        int maxCoord = displayPixelSize - 1;
        for (Map.Entry<Integer, List<ImageProcessor.Triangle>> entry : triangles.entrySet()) {
            List<ImageProcessor.Triangle> triangleList = entry.getValue();
            if (!triangleList.isEmpty()) {
                commands.add(formatColorCommand(entry.getKey()));
                for (ImageProcessor.Triangle t : triangleList) {
                    int raw_x1 = t.x1 - offsetX;
                    int raw_y1 = displayPixelSize - 1 - (t.y1 - offsetY);
                    int raw_x2 = t.x2 - offsetX;
                    int raw_y2 = displayPixelSize - 1 - (t.y2 - offsetY);
                    int raw_x3 = t.x3 - offsetX;
                    int raw_y3 = displayPixelSize - 1 - (t.y3 - offsetY);

                    int x1 = Math.max(0, Math.min(maxCoord, raw_x1));
                    int y1 = Math.max(0, Math.min(maxCoord, raw_y1));
                    int x2 = Math.max(0, Math.min(maxCoord, raw_x2));
                    int y2 = Math.max(0, Math.min(maxCoord, raw_y2));
                    int x3 = Math.max(0, Math.min(maxCoord, raw_x3));
                    int y3 = Math.max(0, Math.min(maxCoord, raw_y3));
                    
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
EOF

# --- 5. Исправляем ModUI.java (исправляем ошибки компиляции в расчете статистики) ---
cat << 'EOF' > src/main/java/com/mkso4ka/mindustry/matrixproc/ModUI.java
package com.mkso4ka.mindustry.matrixproc;

import arc.Core;
import arc.files.Fi;
import arc.math.geom.Point2;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.logic.LogicDisplay;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModUI {

    private static LogicDisplay selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay;
    private static Slider xSlider, ySlider, toleranceSlider, instructionsSlider, diffusionIterSlider, diffusionKSlider;
    private static Label xLabel, yLabel, toleranceLabel, instructionsLabel, diffusionIterLabel, diffusionKLabel;
    private static CheckBox transparentBgCheck;
    private static Table previewTable;
    private static Label availableProcessorsLabel, requiredProcessorsLabel, statusLabel;
    
    private static final AtomicBoolean cancellationToken = new AtomicBoolean(false);

    public static void build() {
        try {
            Table schematicsButtons = Vars.ui.schematics.buttons;
            Cell<TextButton> buttonCell = schematicsButtons.button("PictureToLogic", Icon.image, ModUI::showSettingsDialog);
            buttonCell.size(180, 64).padLeft(6);
        } catch (Exception e) {
            WebLogger.err("Failed to build PictureToLogic UI!", e);
        }
    }

    private static void showSettingsDialog() {
        BaseDialog dialog = new BaseDialog("Настройки PictureToLogic");
        dialog.addCloseButton();
        Table content = dialog.cont;
        content.defaults().pad(4);
        Table mainTable = new Table();
        Table leftPanel = new Table();
        leftPanel.defaults().pad(2).left();

        leftPanel.add("[accent]1. Настройки сетки[]").colspan(3).row();
        xSlider = new Slider(1, 10, 1, false);
        ySlider = new Slider(1, 10, 1, false);
        xLabel = new Label("1"); yLabel = new Label("1");
        leftPanel.add("Дисплеев по X:");
        leftPanel.add(xSlider).width(200f).pad(5);
        leftPanel.add(xLabel).row();
        leftPanel.add("Дисплеев по Y:");
        leftPanel.add(ySlider).width(200f).pad(5);
        leftPanel.add(yLabel).row();

        leftPanel.add("[accent]2. Оптимизация изображения[]").colspan(3).padTop(15).row();
        transparentBgCheck = new CheckBox(" Прозрачный фон");
        transparentBgCheck.setChecked(true);
        leftPanel.add(transparentBgCheck).colspan(3).left().row();
        diffusionIterSlider = new Slider(0, 10, 1, false);
        diffusionIterSlider.setValue(5);
        diffusionIterLabel = new Label("5");
        leftPanel.add("Сила сглаживания:");
        leftPanel.add(diffusionIterSlider).width(200f).pad(5);
        leftPanel.add(diffusionIterLabel).row();
        diffusionKSlider = new Slider(1, 25, 0.5f, false);
        diffusionKSlider.setValue(10);
        diffusionKLabel = new Label("10.0");
        leftPanel.add("Сохранение краев:");
        leftPanel.add(diffusionKSlider).width(200f).pad(5);
        leftPanel.add(diffusionKLabel).row();
        toleranceSlider = new Slider(0, 3, 0.1f, false);
        toleranceSlider.setValue(1.5f);
        toleranceLabel = new Label("1.5");
        leftPanel.add("Допуск цвета (Delta E):");
        leftPanel.add(toleranceSlider).width(200f).pad(5);
        leftPanel.add(toleranceLabel).row();

        leftPanel.add("[accent]3. Настройки вывода[]").colspan(3).padTop(15).row();
        instructionsSlider = new Slider(100, 1000, 100, false);
        instructionsSlider.setValue(1000);
        instructionsLabel = new Label("1000");
        leftPanel.add("Макс. инструкций:");
        leftPanel.add(instructionsSlider).width(200f).pad(5);
        leftPanel.add(instructionsLabel).row();
        
        leftPanel.add("[accent]4. Статистика процессоров[]").colspan(3).padTop(15).row();
        availableProcessorsLabel = new Label("");
        requiredProcessorsLabel = new Label("");
        statusLabel = new Label("");
        leftPanel.add(availableProcessorsLabel).colspan(3).row();
        leftPanel.add(requiredProcessorsLabel).colspan(3).row();
        leftPanel.add(statusLabel).colspan(3).padTop(5).row();

        Table rightPanel = new Table();
        previewTable = new Table();
        previewTable.setBackground(Tex.buttonDown);
        rightPanel.add(previewTable).size(150).padBottom(10).row();
        Table displaySelector = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        TextButton logicDisplayButton = new TextButton("3x3", Styles.togglet);
        logicDisplayButton.clicked(() -> { selectedDisplay = (LogicDisplay) Blocks.logicDisplay; updateProcessorEstimationLabels(); });
        TextButton largeLogicDisplayButton = new TextButton("6x6", Styles.togglet);
        largeLogicDisplayButton.clicked(() -> { selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay; updateProcessorEstimationLabels(); });
        group.add(logicDisplayButton, largeLogicDisplayButton);
        if (selectedDisplay == Blocks.largeLogicDisplay) largeLogicDisplayButton.setChecked(true);
        else logicDisplayButton.setChecked(true);
        displaySelector.add(logicDisplayButton).size(70, 60);
        displaySelector.add(largeLogicDisplayButton).size(70, 60).padLeft(10);
        rightPanel.add("Тип дисплея:").row();
        rightPanel.add(displaySelector).row();

        mainTable.add(leftPanel);
        mainTable.add(rightPanel).padLeft(20);
        content.add(mainTable).row();
        Table bottomPanel = new Table();
        if (WebLogger.ENABLE_WEB_LOGGER) {
            bottomPanel.button("Открыть панель отладки", Icon.zoom, () -> Core.app.openURI("http://localhost:8080/")).growX();
        }
        content.add(bottomPanel).growX().padTop(15).row();
        Runnable fileChooserAction = () -> WebLogger.logFileChooser(file -> {
            if (file != null) {
                dialog.hide();
                generateAndShowSchematic(file);
            }
        });
        content.button("Выбрать и создать чертеж", Icon.file, fileChooserAction).padTop(10).growX().height(60);

        xSlider.changed(() -> { xLabel.setText(String.valueOf((int)xSlider.getValue())); updatePreview(); updateProcessorEstimationLabels(); });
        ySlider.changed(() -> { yLabel.setText(String.valueOf((int)ySlider.getValue())); updatePreview(); updateProcessorEstimationLabels(); });
        toleranceSlider.changed(() -> { toleranceLabel.setText(String.format("%.1f", toleranceSlider.getValue())); updateProcessorEstimationLabels(); });
        instructionsSlider.changed(() -> { instructionsLabel.setText(String.valueOf((int)instructionsSlider.getValue())); updateProcessorEstimationLabels(); });
        diffusionIterSlider.changed(() -> diffusionIterLabel.setText(String.valueOf((int)diffusionIterSlider.getValue())));
        diffusionKSlider.changed(() -> diffusionKLabel.setText(String.format("%.1f", diffusionKSlider.getValue())));
        
        updatePreview();
        updateProcessorEstimationLabels();
        dialog.show();
    }
    
    private static void updateProcessorEstimationLabels() {
        int displaysX = (int) xSlider.getValue();
        int displaysY = (int) ySlider.getValue();
        int displaySize = selectedDisplay.size;
        double tolerance = toleranceSlider.getValue();
        int maxInstructions = (int) instructionsSlider.getValue();
        
        DisplayMatrix displayMatrix = new DisplayMatrix();
        // ИСПРАВЛЕНИЕ: Передаем int вместо double
        MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(displaysX, displaysY, displaySize, (int)DisplayProcessorMatrixFinal.PROCESSOR_REACH);
        DisplayProcessorMatrixFinal tempMatrix = new DisplayProcessorMatrixFinal(blueprint.n, blueprint.m, new int[displaysX*displaysY], blueprint.displayBottomLefts, displaySize);
        
        int availableSlots = 0;
        // ИСПРАВЛЕНИЕ: Правильный цикл для итерации по матрице
        for (int y = 0; y < blueprint.n; y++) {
            for (int x = 0; x < blueprint.m; x++) {
                if (tempMatrix.getMatrix()[y][x].type == 0 && tempMatrix.isWithinProcessorReachOfAnyDisplay(new Point2(x, y))) {
                    availableSlots++;
                }
            }
        }
        availableProcessorsLabel.setText("Максимум доступно процессоров: [accent]" + availableSlots + "[]");

        int maxPoints = (int)(5000 - tolerance * 1500);
        double commandsPerSlice = maxPoints * 2.1; 
        int requiredPerSlice = (int)Math.ceil(commandsPerSlice / (maxInstructions - 1));
        int totalRequired = requiredPerSlice * displaysX * displaysY;
        requiredProcessorsLabel.setText("Примерно потребуется: [accent]" + totalRequired + "[]");

        if (totalRequired <= availableSlots) {
            statusLabel.setText("Статус: [green]OK, места должно хватить.[]");
        } else if (totalRequired <= availableSlots * 1.2) {
            statusLabel.setText("Статус: [yellow]ВНИМАНИЕ! Может не хватить места.[]");
        } else {
            statusLabel.setText("Статус: [red]ОШИБКА! Места точно не хватит.[]");
        }
    }

    private static void updatePreview() {
        int x = (int) xSlider.getValue();
        int y = (int) ySlider.getValue();
        previewTable.clear();
        for (int i = 0; i < y; i++) {
            for (int j = 0; j < x; j++) {
                previewTable.add(new Image(Styles.black6)).size(24).pad(2);
            }
            previewTable.row();
        }
    }

    private static void generateAndShowSchematic(Fi imageFile) {
        BaseDialog progressDialog = new BaseDialog("Обработка");
        progressDialog.cont.add("Идет обработка изображения...").pad(20).row();
        progressDialog.buttons.button("Отмена", Icon.cancel, () -> {
            cancellationToken.set(true);
            progressDialog.hide();
        }).size(200, 50);
        progressDialog.show();

        cancellationToken.set(false);

        new Thread(() -> {
            ProcessingResult result = null;
            try {
                int displaysX = (int) xSlider.getValue();
                int displaysY = (int) ySlider.getValue();
                double tolerance = toleranceSlider.getValue();
                int maxInstructions = (int)instructionsSlider.getValue();
                int diffusionIterations = (int)diffusionIterSlider.getValue();
                float diffusionContrast = diffusionKSlider.getValue();
                boolean useTransparentBg = transparentBgCheck.isChecked();

                LogicCore logic = new LogicCore();
                result = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay, tolerance, maxInstructions, diffusionIterations, diffusionContrast, useTransparentBg, cancellationToken);
            } catch (Exception e) {
                WebLogger.err("Критическая ошибка при создании чертежа!", e);
            } finally {
                progressDialog.hide();
                
                ProcessingResult finalResult = result;
                Core.app.post(() -> {
                    if (cancellationToken.get()) {
                        WebLogger.info("Processing was cancelled by the user. No schematic will be shown.");
                        return;
                    }

                    if (finalResult != null && finalResult.schematic != null) {
                        showConfirmationDialog(finalResult);
                    } else {
                        Vars.ui.showInfo("[red]Не удалось создать чертеж.[]\nПроверьте логи для получения подробной информации.");
                        WebLogger.err("Failed to create schematic. Check logs for details.");
                    }
                });
            }
        }).start();
    }
    
    private static void showConfirmationDialog(ProcessingResult result) {
        BaseDialog confirmDialog = new BaseDialog("Результат");
        
        int processorCount = (int) Arrays.stream(result.matrix).flatMap(Arrays::stream).filter(c -> c.type == 1 && c.processorIndex >= 0).count();
        
        Table cont = confirmDialog.cont;
        cont.add("[accent]Чертеж готов![]").colspan(2).row();
        cont.add("Размер схемы:").left().padTop(10);
        cont.add(String.format("%d x %d", result.matrixWidth, result.matrixHeight)).right().row();
        cont.add("Количество дисплеев:").left();
        cont.add(String.valueOf(result.displays.length)).right().row();
        cont.add("Количество процессоров:").left();
        cont.add(String.valueOf(processorCount)).right().row();
        
        confirmDialog.buttons.button("Разместить чертеж", Icon.ok, () -> {
            confirmDialog.hide();
            Vars.ui.schematics.hide();
            Vars.control.input.useSchematic(result.schematic);
            WebLogger.info("Schematic built and placed successfully.");
        }).size(240, 50);
        
        confirmDialog.buttons.button("Отмена", Icon.cancel, confirmDialog::hide).size(160, 50);
        
        confirmDialog.show();
    }
}
EOF

echo "Все файлы успешно обновлены! Пожалуйста, попробуйте запустить сборку снова."
