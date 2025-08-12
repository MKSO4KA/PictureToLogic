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
