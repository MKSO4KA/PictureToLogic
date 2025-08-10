package com.mkso4ka.mindustry.matrixproc;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.logic.LogicDisplay;
import mindustry.content.Blocks;

public class ModUI {

    private static TextField displaysXField;
    private static TextField displaysYField;
    private static LogicDisplay selectedDisplay = (LogicDisplay) Blocks.largeLogicDisplay;

    public static void build() {
        try {
            Table schematicsButtons = Vars.ui.schematics.buttons;
            schematicsButtons.button("PictureToLogic", Icon.image, ModUI::showSettingsDialog).size(180, 64).padLeft(6);
        } catch (Exception e) {
            Log.err("Failed to build PictureToLogic UI!", e);
        }
    }

    private static void showSettingsDialog() {
        BaseDialog dialog = new BaseDialog("Настройки PictureToLogic");

        Table content = dialog.cont;
        content.defaults().pad(4);

        content.add("Дисплеев по X:").padRight(10);
        displaysXField = new TextField("1");
        displaysXField.setValidator(text -> text.matches("[0-9]+") && Integer.parseInt(text) > 0);
        content.add(displaysXField).width(100f).row();

        content.add("Дисплеев по Y:").padRight(10);
        displaysYField = new TextField("1");
        displaysYField.setValidator(text -> text.matches("[0-9]+") && Integer.parseInt(text) > 0);
        content.add(displaysYField).width(100f).row();

        content.add("Тип дисплея:").colspan(2).left().row();
        Table displaySelector = new Table();
        Seq<LogicDisplay> displays = Vars.content.blocks().select(b -> b instanceof LogicDisplay).as();
        ItemSelection.buildTable(displaySelector, displays, () -> selectedDisplay, d -> {
            selectedDisplay = d;
        });
        content.add(displaySelector).colspan(2).left().row();

        content.button("Выбрать и создать чертеж", Icon.file, () -> {
            Vars.platform.showFileChooser(true, "Выбор изображения", "png", file -> {
                if (file != null) {
                    dialog.hide();
                    generateAndShowSchematic(file);
                } else {
                    Vars.ui.showInfo("Файл не выбран.");
                }
            });
        }).padTop(20).colspan(2).growX();

        dialog.buttons.button("Закрыть", Icon.cancel, dialog::hide).size(150, 54);
        dialog.show();
    }

    private static void generateAndShowSchematic(Fi imageFile) {
        Vars.ui.loadfrag.show("Обработка изображения...");

        new Thread(() -> {
            ProcessingResult result = null;
            try {
                int displaysX = Integer.parseInt(displaysXField.getText());
                int displaysY = Integer.parseInt(displaysYField.getText());

                LogicCore logic = new LogicCore();
                result = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay);
            } catch (Exception e) {
                Log.err("Критическая ошибка при создании чертежа!", e);
            } finally {
                Vars.ui.loadfrag.hide();
                
                ProcessingResult finalResult = result;
                Core.app.post(() -> {
                    if (finalResult != null && finalResult.schematic != null) {
                        showDebugDialog(finalResult);
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
        // ИСПРАВЛЕНО: Удалена строка, вызывавшая ошибку компиляции.
        // label.setStyle(Styles.monoLabel); // <-- ОШИБОЧНАЯ СТРОКА
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