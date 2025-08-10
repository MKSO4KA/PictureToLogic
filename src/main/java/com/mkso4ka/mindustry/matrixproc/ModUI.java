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

    private static Slider xSlider, ySlider, instructionsSlider;
    private static Label xLabel, yLabel, instructionsLabel;
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

        // --- Верхняя панель: Слайдеры и предпросмотр ---
        Table topPanel = new Table();
        Table sliders = new Table();
        sliders.defaults().pad(2);

        // Слайдер для X
        xSlider = WebLogger.logChange(new Slider(1, 10, 1, false), "Displays X");
        xLabel = new Label("1");
        sliders.add("Дисплеев по X:").left();
        sliders.add(xSlider).width(200f).padLeft(10).padRight(10);
        sliders.add(xLabel).left().minWidth(25);
        sliders.row();

        // Слайдер для Y
        ySlider = WebLogger.logChange(new Slider(1, 10, 1, false), "Displays Y");
        yLabel = new Label("1");
        sliders.add("Дисплеев по Y:").left();
        sliders.add(ySlider).width(200f).padLeft(10).padRight(10);
        sliders.add(yLabel).left().minWidth(25);
        sliders.row();

        // --- НОВЫЙ СЛАЙДЕР: Инструкций на процессор ---
        instructionsSlider = WebLogger.logChange(new Slider(1, 10, 1, false), "Instructions per Processor");
        instructionsSlider.setValue(10); // По умолчанию 10 * 100 = 1000
        instructionsLabel = new Label("1000");
        sliders.add("Инструкций/проц:").left();
        sliders.add(instructionsSlider).width(200f).padLeft(10).padRight(10);
        sliders.add(instructionsLabel).left().minWidth(40); // Больше места для четырехзначного числа
        sliders.row();

        // Панель предпросмотра
        previewTable = new Table();
        previewTable.setBackground(Tex.buttonDown);

        // ИСПРАВЛЕНО: Добавляем sliders в отдельную ячейку, чтобы предпросмотр не влиял на них
        topPanel.add(sliders);
        topPanel.add(previewTable).padLeft(20).top(); // .top() чтобы выровнять по верху
        content.add(topPanel).row();

        // --- Средняя панель: Выбор типа дисплея ---
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

        // --- Нижняя панель: Опции и действия ---
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

        // --- Инициализация и слушатели ---
        xSlider.changed(() -> {
            xLabel.setText(String.valueOf((int)xSlider.getValue()));
            updatePreview();
        });
        ySlider.changed(() -> {
            yLabel.setText(String.valueOf((int)ySlider.getValue()));
            updatePreview();
        });
        // Слушатель для нового слайдера
        instructionsSlider.changed(() -> {
            instructionsLabel.setText(String.valueOf((int)instructionsSlider.getValue() * 100));
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
        
        // Получаем значения из UI
        int displaysX = (int) xSlider.getValue();
        int displaysY = (int) ySlider.getValue();
        int instructionsPerProc = (int) instructionsSlider.getValue() * 100;

        WebLogger.info("--- Starting Image Processing ---");
        WebLogger.info("File: %s", imageFile.name());
        WebLogger.info("Grid: %dx%d", displaysX, displaysY);
        WebLogger.info("Display Type: %s", selectedDisplay.name);
        WebLogger.info("Instructions per Processor: %d", instructionsPerProc);

        new Thread(() -> {
            ProcessingResult result = null;
            try {
                LogicCore logic = new LogicCore();
                // Передаем новое значение в ядро логики
                result = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay, instructionsPerProc);
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