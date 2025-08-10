package com.mkso4ka.mindustry.matrixproc;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Slider;
import arc.scene.ui.TextButton;
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

    // ИСПОЛЬЗУЕМ largeLogicDisplay ПО УМОЛЧАНИЮ
    private static LogicDisplay selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay; 
    private static boolean showDebug = true;

    private static Slider xSlider, ySlider;
    private static Label xLabel, yLabel;
    private static Table previewTable;

    public static void build() {
        try {
            Table schematicsButtons = Vars.ui.schematics.buttons;
            schematicsButtons.button("PictureToLogic", Icon.image, ModUI::showSettingsDialog).size(180, 64).padLeft(6);
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
        sliders.defaults().pad(2);

        xSlider = new Slider(1, 10, 1, false);
        xLabel = new Label("1");
        sliders.add("Дисплеев по X:").left();
        sliders.add(xSlider).width(200f).padLeft(10).padRight(10);
        sliders.add(xLabel).left();
        sliders.row();

        ySlider = new Slider(1, 10, 1, false);
        yLabel = new Label("1");
        sliders.add("Дисплеев по Y:").left();
        sliders.add(ySlider).width(200f).padLeft(10).padRight(10);
        sliders.add(yLabel).left();

        previewTable = new Table();
        previewTable.setBackground(Tex.buttonDown);

        topPanel.add(sliders);
        topPanel.add(previewTable).padLeft(20);
        content.add(topPanel).row();

        content.add("Тип дисплея:").left().padTop(20).row();
        Table displaySelector = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);

        // Кнопка для 3x3 дисплея (logicDisplay)
        TextButton logicDisplayButton = new TextButton("Логический дисплей (3x3)", Styles.togglet);
        logicDisplayButton.clicked(() -> selectedDisplay = (LogicDisplay) Blocks.logicDisplay);
        
        // Кнопка для 6x6 дисплея (largeLogicDisplay)
        TextButton largeLogicDisplayButton = new TextButton("Большой дисплей (6x6)", Styles.togglet);
        largeLogicDisplayButton.clicked(() -> selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay);
        
        group.add(logicDisplayButton, largeLogicDisplayButton);
        
        // Устанавливаем, какая кнопка будет выбрана по умолчанию
        if (selectedDisplay == Blocks.largeLogicDisplay) {
            largeLogicDisplayButton.setChecked(true);
        } else {
            logicDisplayButton.setChecked(true);
        }

        displaySelector.add(logicDisplayButton).size(240, 60);
        displaySelector.add(largeLogicDisplayButton).size(240, 60).padLeft(10);
        content.add(displaySelector).row();

        content.check("Показывать отладочное окно", showDebug, b -> showDebug = b).left().padTop(20).row();

        content.button("Выбрать и создать чертеж", Icon.file, () -> {
            Vars.platform.showFileChooser(true, "Выбор изображения", "png", file -> {
                if (file != null) {
                    dialog.hide();
                    generateAndShowSchematic(file);
                } else {
                    Vars.ui.showInfo("Файл не выбран.");
                }
            });
        }).padTop(20).growX().height(60);

        xSlider.changed(() -> {
            xLabel.setText(String.valueOf((int)xSlider.getValue()));
            updatePreview();
        });
        ySlider.changed(() -> {
            yLabel.setText(String.valueOf((int)ySlider.getValue()));
            updatePreview();
        });
        updatePreview();

        dialog.show();
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

        new Thread(() -> {
            ProcessingResult result = null;
            try {
                int displaysX = (int) xSlider.getValue();
                int displaysY = (int) ySlider.getValue();

                LogicCore logic = new LogicCore();
                result = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay);
            } catch (Exception e) {
                WebLogger.err("Критическая ошибка при создании чертежа!", e);
            } finally {
                Vars.ui.loadfrag.hide();
                
                ProcessingResult finalResult = result;
                Core.app.post(() -> {
                    if (finalResult != null && finalResult.schematic != null) {
                        if (showDebug) {
                            showDebugDialog(finalResult);
                        } else {
                            Vars.ui.schematics.hide();
                            Vars.control.input.useSchematic(finalResult.schematic);
                        }
                    } else {
                        Vars.ui.showInfo("[scarlet]Не удалось создать чертеж. Проверьте логи.[]");
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

        dialog.show();
    }
}