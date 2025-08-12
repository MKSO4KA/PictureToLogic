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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModUI {

    private static LogicDisplay selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay;
    private static Slider xSlider, ySlider, toleranceSlider, instructionsSlider;
    private static Label xLabel, yLabel, toleranceLabel, instructionsLabel, pointsLabel;
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
        leftPanel.add("[accent]2. Настройки качества[]").colspan(3).padTop(15).row();
        toleranceSlider = new Slider(0.1f, 6f, 0.1f, false);
        toleranceSlider.setValue(1.0f);
        toleranceLabel = new Label("1.0");
        pointsLabel = new Label("");
        leftPanel.add("Детализация:");
        leftPanel.add(toleranceSlider).width(200f).pad(5);
        leftPanel.add(toleranceLabel).row();
        leftPanel.add();
        leftPanel.add(pointsLabel).colspan(2).left().padLeft(5).row();
        transparentBgCheck = new CheckBox(" Прозрачный фон");
        transparentBgCheck.setChecked(true);
        leftPanel.add(transparentBgCheck).colspan(3).left().padTop(5).row();
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
        toleranceSlider.changed(ModUI::updateDetailLabels);
        instructionsSlider.changed(() -> { instructionsLabel.setText(String.valueOf((int)instructionsSlider.getValue())); updateProcessorEstimationLabels(); });
        updatePreview();
        updateDetailLabels();
        dialog.show();
    }
    
    private static void updateDetailLabels() {
        float detailValue = toleranceSlider.getValue();
        int maxPoints = (int)(500 + detailValue * 1500);
        toleranceLabel.setText(String.format("%.1f", detailValue));
        pointsLabel.setText(String.format("[gray](Примерно %d точек на слайс)[]", maxPoints));
        updateProcessorEstimationLabels();
    }
    
    private static void updateProcessorEstimationLabels() {
        int displaysX = (int) xSlider.getValue();
        int displaysY = (int) ySlider.getValue();
        int displaySize = selectedDisplay.size;
        float detailValue = toleranceSlider.getValue();
        int maxInstructions = (int) instructionsSlider.getValue();
        int availableSlots = DisplayProcessorMatrixFinal.calculateMaxAvailableProcessors(displaysX, displaysY, displaySize);
        availableProcessorsLabel.setText("Максимум доступно процессоров: [accent]" + availableSlots + "[]");
        int maxPoints = (int)(500 + detailValue * 1500);
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
                double detail = toleranceSlider.getValue();
                int maxInstructions = (int)instructionsSlider.getValue();
                boolean useTransparentBg = transparentBgCheck.isChecked();

                LogicCore logic = new LogicCore();
                result = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay, detail, maxInstructions, useTransparentBg, cancellationToken);
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
        cont.add(String.format("%d x %d", result.schematic.width, result.schematic.height)).right().row();
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