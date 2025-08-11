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

    private static Slider xSlider, ySlider, toleranceSlider, instructionsSlider, diffusionIterSlider, diffusionKSlider;
    private static Label xLabel, yLabel, toleranceLabel, instructionsLabel, diffusionIterLabel, diffusionKLabel;
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

        toleranceSlider = WebLogger.logChange(new Slider(0, 3, 0.1f, false), "Color Tolerance");
        toleranceSlider.setValue(1.5f);
        toleranceLabel = new Label("1.5");
        leftPanel.add("Допуск цвета (Delta E):");
        leftPanel.add(toleranceSlider).width(200f).pad(5);
        leftPanel.add(toleranceLabel).row();

        leftPanel.add("[accent]3. Настройки вывода[]").colspan(3).padTop(15).row();
        instructionsSlider = WebLogger.logChange(new Slider(100, 1000, 100, false), "Max Instructions");
        instructionsSlider.setValue(1000);
        instructionsLabel = new Label("1000");
        leftPanel.add("Макс. инструкций:");
        leftPanel.add(instructionsSlider).width(200f).pad(5);
        leftPanel.add(instructionsLabel).row();

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

        Table bottomPanel = new Table();
        if (WebLogger.ENABLE_WEB_LOGGER) {
            // ИЗМЕНЕНИЕ: Кнопка теперь ведет на главную страницу отладки
            bottomPanel.button("Открыть панель отладки", Icon.zoom, () -> Core.app.openURI("http://localhost:8080/")).growX();
        }
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

        xSlider.changed(() -> { xLabel.setText(String.valueOf((int)xSlider.getValue())); updatePreview(); });
        ySlider.changed(() -> { yLabel.setText(String.valueOf((int)ySlider.getValue())); updatePreview(); });
        toleranceSlider.changed(() -> toleranceLabel.setText(String.format("%.1f", toleranceSlider.getValue())));
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
        WebLogger.info("Max Instructions (User): %d", (int)instructionsSlider.getValue());
        WebLogger.info("Diffusion Iterations: %d", (int)diffusionIterSlider.getValue());
        WebLogger.info("Diffusion K-value: %.1f", diffusionKSlider.getValue());

        new Thread(() -> {
            ProcessingResult result = null;
            try {
                int displaysX = (int) xSlider.getValue();
                int displaysY = (int) ySlider.getValue();
                double tolerance = toleranceSlider.getValue();
                int maxInstructions = (int)instructionsSlider.getValue() - 11;
                int diffusionIterations = (int)diffusionIterSlider.getValue();
                float diffusionContrast = diffusionKSlider.getValue();

                LogicCore logic = new LogicCore();
                result = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay, tolerance, maxInstructions, diffusionIterations, diffusionContrast);
            } catch (Exception e) {
                WebLogger.err("Критическая ошибка при создании чертежа!", e);
            } finally {
                Vars.ui.loadfrag.hide();
                WebLogger.info("--- Image Processing Finished ---");
                
                ProcessingResult finalResult = result;
                Core.app.post(() -> {
                    if (finalResult != null && finalResult.schematic != null) {
                        Vars.ui.schematics.hide();
                        Vars.control.input.useSchematic(finalResult.schematic);
                        WebLogger.info("Schematic built successfully.");
                    } else {
                        WebLogger.err("Failed to create schematic. Check logs for details.");
                    }
                });
            }
        }).start();
    }
}
