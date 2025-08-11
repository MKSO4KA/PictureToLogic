package com.mkso4ka.mindustry.matrixproc;

import arc.Core;
import arc.files.Fi;
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

public class ModUI {

    private static LogicDisplay selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay;
    private static boolean showDebug = true;

    private static Slider xSlider, ySlider, toleranceSlider, luminanceSlider, instructionsSlider, diffusionIterSlider, diffusionKSlider;
    private static Label xLabel, yLabel, toleranceLabel, luminanceLabel, instructionsLabel, diffusionIterLabel, diffusionKLabel;
    private static Table previewTable;

    public static void build() {
        try {
            Table schematicsButtons = Vars.ui.schematics.buttons;
            Cell<TextButton> buttonCell = schematicsButtons.button("PictureToLogic", Icon.image, ModUI::showSettingsDialog);
            WebLogger.logClick(buttonCell.get(), "Open Settings");
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

        // --- Секция 1: Сетка ---
        leftPanel.add("[accent]1. Настройки сетки[]").colspan(3).row();
        xSlider = WebLogger.logChange(new Slider(1, 10, 1, false), "Displays X");
        ySlider = WebLogger.logChange(new Slider(1, 10, 1, false), "Displays Y");
        xLabel = new Label("1"); yLabel = new Label("1");
        leftPanel.add("Дисплеев по X:");
        leftPanel.add(xSlider).width(200f).pad(5);
        leftPanel.add(xLabel).row();
        leftPanel.add("Дисплеев по Y:");
        leftPanel.add(ySlider).width(200f).pad(5);
        leftPanel.add(yLabel).row();

        // --- Секция 2: Оптимизация изображения ---
        leftPanel.add("[accent]2. Оптимизация изображения[]").colspan(3).padTop(15).row();
        diffusionIterSlider = WebLogger.logChange(new Slider(0, 10, 1, false), "Diffusion Iterations");
        diffusionIterSlider.setValue(5);
        diffusionIterLabel = new Label("5");
        leftPanel.add("Сила сглаживания:");
        leftPanel.add(diffusionIterSlider).width(200f).pad(5);
        leftPanel.add(diffusionIterLabel).row();

        diffusionKSlider = WebLogger.logChange(new Slider(1, 25, 0.5f, false), "Edge Threshold");
        diffusionKSlider.setValue(10);
        diffusionKLabel = new Label("10.0");
        leftPanel.add("Сохранение краев:");
        leftPanel.add(diffusionKSlider).width(200f).pad(5);
        leftPanel.add(diffusionKLabel).row();

        toleranceSlider = WebLogger.logChange(new Slider(0, 50, 1, false), "Color Tolerance");
        toleranceSlider.setValue(10);
        toleranceLabel = new Label("10.0");
        leftPanel.add("Допуск цвета (Delta E):");
        leftPanel.add(toleranceSlider).width(200f).pad(5);
        leftPanel.add(toleranceLabel).row();

        luminanceSlider = WebLogger.logChange(new Slider(0, 3, 0.1f, false), "Luminance Weight");
        luminanceSlider.setValue(1);
        luminanceLabel = new Label("1.0");
        leftPanel.add("Вес яркости (L*):");
        leftPanel.add(luminanceSlider).width(200f).pad(5);
        leftPanel.add(luminanceLabel).row();

        // --- Секция 3: Настройки вывода ---
        leftPanel.add("[accent]3. Настройки вывода[]").colspan(3).padTop(15).row();
        instructionsSlider = WebLogger.logChange(new Slider(100, 1000, 100, false), "Max Instructions");
        instructionsSlider.setValue(1000);
        instructionsLabel = new Label("1000");
        leftPanel.add("Макс. инструкций:");
        leftPanel.add(instructionsSlider).width(200f).pad(5);
        leftPanel.add(instructionsLabel).row();

        // --- Правая панель ---
        Table rightPanel = new Table();
        previewTable = new Table();
        previewTable.setBackground(Tex.buttonDown);
        rightPanel.add(previewTable).size(150).padBottom(10).row();
        
        Table displaySelector = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        TextButton logicDisplayButton = WebLogger.logClick(new TextButton("3x3", Styles.togglet), "Select 3x3");
        logicDisplayButton.clicked(() -> selectedDisplay = (LogicDisplay) Blocks.logicDisplay);
        TextButton largeLogicDisplayButton = WebLogger.logClick(new TextButton("6x6", Styles.togglet), "Select 6x6");
        largeLogicDisplayButton.clicked(() -> selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay);
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

        // --- Нижняя панель ---
        Table bottomPanel = new Table();
        if (WebLogger.ENABLE_WEB_LOGGER) {
            bottomPanel.button("Визуальный отладчик", Icon.zoom, () -> Core.app.openURI("http://localhost:8080/debug")).left();
        }
        CheckBox debugCheckBox = new CheckBox("Отладочное окно");
        debugCheckBox.setChecked(showDebug);
        debugCheckBox.changed(() -> showDebug = debugCheckBox.isChecked());
        bottomPanel.add(WebLogger.logToggle(debugCheckBox, "Show Debug Window")).expandX().right();
        content.add(bottomPanel).growX().padTop(15).row();

        Runnable fileChooserAction = () -> WebLogger.logFileChooser(file -> {
            if (file != null) {
                dialog.hide();
                generateAndShowSchematic(file);
            }
        });
        Cell<TextButton> selectFileCell = content.button("Выбрать и создать чертеж", Icon.file, fileChooserAction);
        WebLogger.logClick(selectFileCell.get(), "Select Image and Create");
        selectFileCell.padTop(10).growX().height(60);

        // --- Слушатели ---
        xSlider.changed(() -> { xLabel.setText(String.valueOf((int)xSlider.getValue())); updatePreview(); });
        ySlider.changed(() -> { yLabel.setText(String.valueOf((int)ySlider.getValue())); updatePreview(); });
        toleranceSlider.changed(() -> toleranceLabel.setText(String.format("%.1f", toleranceSlider.getValue())));
        luminanceSlider.changed(() -> luminanceLabel.setText(String.format("%.1f", luminanceSlider.getValue())));
        instructionsSlider.changed(() -> instructionsLabel.setText(String.valueOf((int)instructionsSlider.getValue())));
        diffusionIterSlider.changed(() -> diffusionIterLabel.setText(String.valueOf((int)diffusionIterSlider.getValue())));
        diffusionKSlider.changed(() -> diffusionKLabel.setText(String.format("%.1f", diffusionKSlider.getValue())));
        updatePreview();

        WebLogger.logShow(dialog, "Settings Dialog");
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
        Vars.ui.loadfrag.show("Обработка изображения...");
        WebLogger.info("--- Starting Image Processing ---");
        WebLogger.info("File: %s", imageFile.name());
        WebLogger.info("Grid: %dx%d", (int)xSlider.getValue(), (int)ySlider.getValue());
        WebLogger.info("Display Type: %s", selectedDisplay.name);
        WebLogger.info("Color Tolerance (Delta E): %.1f", toleranceSlider.getValue());
        WebLogger.info("Luminance Weight: %.1f", luminanceSlider.getValue());
        WebLogger.info("Max Instructions (User): %d", (int)instructionsSlider.getValue());
        WebLogger.info("Diffusion Iterations: %d", (int)diffusionIterSlider.getValue());
        WebLogger.info("Diffusion K-value: %.1f", diffusionKSlider.getValue());

        new Thread(() -> {
            ProcessingResult result = null;
            try {
                int displaysX = (int) xSlider.getValue();
                int displaysY = (int) ySlider.getValue();
                double tolerance = toleranceSlider.getValue();
                float luminanceWeight = luminanceSlider.getValue();
                int maxInstructions = (int)instructionsSlider.getValue() - 11;
                int diffusionIterations = (int)diffusionIterSlider.getValue();
                float diffusionContrast = diffusionKSlider.getValue();

                LogicCore logic = new LogicCore();
                result = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay, tolerance, luminanceWeight, maxInstructions, diffusionIterations, diffusionContrast);
            } catch (Exception e) {
                WebLogger.err("Критическая ошибка при создании чертежа!", e);
            } finally {
                Vars.ui.loadfrag.hide();
                WebLogger.info("--- Image Processing Finished ---");
                
                ProcessingResult finalResult = result;
                Core.app.post(() -> {
                    if (finalResult != null && finalResult.schematic != null) {
                        if (showDebug) {
                            showDebugDialog(finalResult);
                        } else {
                            Vars.ui.schematics.hide();
                            Vars.control.input.useSchematic(finalResult.schematic);
                            WebLogger.info("Schematic built successfully.");
                        }
                    } else {
                        WebLogger.err("Failed to create schematic. Check logs for details.");
                    }
                });
            }
        }).start();
    }

    private static void showDebugDialog(ProcessingResult result) {
        BaseDialog dialog = new BaseDialog("Отладка размещения");
        
        StringBuilder sb = new StringBuilder();
        sb.append("[lightgray]Матрица: ").append(result.matrixWidth).append("x").append(result.matrixHeight).append("\n");
        sb.append("Размер дисплея: ").append(result.displaySize).append("x").append(result.displaySize).append("\n\n");
        
        sb.append("[accent]Информация по дисплеям:[]\n");
        for (DisplayInfo display : result.displays) {
            sb.append("  ID: ").append(display.id);
            sb.append(" | BL: (").append(display.bottomLeft.x).append(",").append(display.bottomLeft.y).append(")");
            sb.append(" | Требуется: ").append(display.totalProcessorsRequired);
            sb.append(" | Размещено: ").append(display.processorsPlaced);
            if (display.getProcessorsNeeded() > 0) {
                sb.append(" [scarlet](НЕХВАТКА)[]");
            }
            sb.append("\n");
        }
        sb.append("\n[accent]Визуализация матрицы (P-процессор, D-дисплей):[]\n");

        for (int y = result.matrixHeight - 1; y >= 0; y--) {
            for (int x = 0; x < result.matrixWidth; x++) {
                DisplayProcessorMatrixFinal.Cell cell = result.matrix[y][x];
                switch(cell.type) {
                    case 1:
                        sb.append("[#").append(mindustry.graphics.Pal.accent.toString()).append("]P[]");
                        break;
                    case 2:
                        sb.append("[#").append(mindustry.graphics.Pal.items.toString()).append("]D[]");
                        break;
                    default:
                        sb.append("[lightgray].[]");
                        break;
                }
            }
            sb.append("\n");
        }
        
        final String debugText = sb.toString();

        Table content = dialog.cont;
        Label label = new Label(debugText);
        ScrollPane scroll = new ScrollPane(label, Styles.defaultPane);
        content.add(scroll).grow().width(Core.graphics.getWidth() * 0.8f).height(Core.graphics.getHeight() * 0.7f);

        dialog.buttons.button("Копировать", Icon.copy, () -> {
            Core.app.setClipboardText(label.getText().toString());
            Vars.ui.showInfo("Отладочная информация скопирована.");
        }).size(200, 54);

        dialog.buttons.button("Построить схему", Icon.ok, () -> {
            dialog.hide();
            Vars.ui.schematics.hide();
            Vars.control.input.useSchematic(result.schematic);
        }).size(220, 54);
        
        dialog.buttons.button("Отмена", Icon.cancel, dialog::hide).size(150, 54);

        WebLogger.logShow(dialog, "Debug Dialog");
    }
}