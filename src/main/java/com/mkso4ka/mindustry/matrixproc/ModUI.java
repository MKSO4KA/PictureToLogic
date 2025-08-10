package com.mkso4ka.mindustry.matrixproc;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.*;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
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

    private static Slider xSlider, ySlider, toleranceSlider, instructionsSlider;
    private static Label xLabel, yLabel, toleranceLabel, instructionsLabel;
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
        content.defaults().pad(8);

        Table topPanel = new Table();
        Table sliders = new Table();
        sliders.defaults().pad(2).left();

        xSlider = WebLogger.logChange(new Slider(1, 10, 1, false), "Displays X");
        ySlider = WebLogger.logChange(new Slider(1, 10, 1, false), "Displays Y");
        xLabel = new Label("1");
        yLabel = new Label("1");

        sliders.add("Дисплеев по X:");
        sliders.add(xSlider).width(200f).padLeft(10).padRight(10);
        sliders.add(xLabel);
        sliders.row();

        sliders.add("Дисплеев по Y:");
        sliders.add(ySlider).width(200f).padLeft(10).padRight(10);
        sliders.add(yLabel);
        sliders.row();

        toleranceSlider = WebLogger.logChange(new Slider(0, 50, 1, false), "Color Tolerance");
        toleranceSlider.setValue(10);
        toleranceLabel = new Label("10.0");
        sliders.add("Допуск цвета:").padTop(10);
        sliders.add(toleranceSlider).width(200f).padLeft(10).padRight(10).padTop(10);
        sliders.add(toleranceLabel).padTop(10);
        sliders.row();

        // --- НОВЫЙ СЛАЙДЕР ДЛЯ КОЛИЧЕСТВА ИНСТРУКЦИЙ ---
        instructionsSlider = WebLogger.logChange(new Slider(100, 1000, 100, false), "Max Instructions");
        instructionsSlider.setValue(1000); // По умолчанию максимум
        instructionsLabel = new Label("1000");
        sliders.add("Макс. инструкций:").padTop(10);
        sliders.add(instructionsSlider).width(200f).padLeft(10).padRight(10).padTop(10);
        sliders.add(instructionsLabel).padTop(10);
        // ------------------------------------------------

        previewTable = new Table();
        previewTable.setBackground(Tex.buttonDown);

        topPanel.add(sliders);
        topPanel.add(previewTable).padLeft(20);
        content.add(topPanel).row();

        content.add("Тип дисплея:").left().padTop(20).row();
        Table displaySelector = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);

        TextButton logicDisplayButton = WebLogger.logClick(
            new TextButton("Логический дисплей (3x3)", Styles.togglet),
            "Select Logic Display (3x3)"
        );
        logicDisplayButton.clicked(() -> selectedDisplay = (LogicDisplay) Blocks.logicDisplay);
        
        TextButton largeLogicDisplayButton = WebLogger.logClick(
            new TextButton("Большой дисплей (6x6)", Styles.togglet),
            "Select Large Logic Display (6x6)"
        );
        largeLogicDisplayButton.clicked(() -> selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay);
        
        group.add(logicDisplayButton, largeLogicDisplayButton);
        
        if (selectedDisplay == Blocks.largeLogicDisplay) {
            largeLogicDisplayButton.setChecked(true);
        } else {
            logicDisplayButton.setChecked(true);
        }

        displaySelector.add(logicDisplayButton).size(240, 60);
        displaySelector.add(largeLogicDisplayButton).size(240, 60).padLeft(10);
        content.add(displaySelector).row();

        CheckBox debugCheckBox = new CheckBox("Показывать отладочное окно");
        debugCheckBox.setChecked(showDebug);
        debugCheckBox.changed(() -> showDebug = debugCheckBox.isChecked());
        content.add(WebLogger.logToggle(debugCheckBox, "Show Debug Window")).left().padTop(20).row();

        Runnable fileChooserAction = () -> WebLogger.logFileChooser(file -> {
            if (file != null) {
                dialog.hide();
                generateAndShowSchematic(file);
            }
        });
        Cell<TextButton> selectFileCell = content.button("Выбрать и создать чертеж", Icon.file, fileChooserAction);
        WebLogger.logClick(selectFileCell.get(), "Select Image and Create");
        selectFileCell.padTop(20).growX().height(60);

        xSlider.changed(() -> {
            xLabel.setText(String.valueOf((int)xSlider.getValue()));
            updatePreview();
        });
        ySlider.changed(() -> {
            yLabel.setText(String.valueOf((int)ySlider.getValue()));
            updatePreview();
        });
        toleranceSlider.changed(() -> {
            toleranceLabel.setText(String.format("%.1f", toleranceSlider.getValue()));
        });
        instructionsSlider.changed(() -> {
            instructionsLabel.setText(String.valueOf((int)instructionsSlider.getValue()));
        });
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

        new Thread(() -> {
            ProcessingResult result = null;
            try {
                int displaysX = (int) xSlider.getValue();
                int displaysY = (int) ySlider.getValue();
                double tolerance = toleranceSlider.getValue();
                // Применяем твое правило: пользовательское значение минус 11
                int maxInstructions = (int)instructionsSlider.getValue() - 11;

                LogicCore logic = new LogicCore();
                result = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay, tolerance, maxInstructions);
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