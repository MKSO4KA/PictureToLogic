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
import mindustry.content.Blocks; // Импортируем для дисплея по умолчанию

public class ModUI {

    private static TextField displaysXField;
    private static TextField displaysYField;
    // НОВАЯ ПЕРЕМЕННАЯ: Хранит выбранный пользователем дисплей
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

        // --- НАЧАЛО ИЗМЕНЕНИЙ ---

        Table content = dialog.cont;
        content.defaults().pad(4);

        // Поля для ввода размеров сетки
        content.add("Дисплеев по X:").padRight(10);
        displaysXField = new TextField("1");
        displaysXField.setValidator(text -> text.matches("[0-9]+") && Integer.parseInt(text) > 0);
        content.add(displaysXField).width(100f).row();

        content.add("Дисплеев по Y:").padRight(10);
        displaysYField = new TextField("1");
        displaysYField.setValidator(text -> text.matches("[0-9]+") && Integer.parseInt(text) > 0);
        content.add(displaysYField).width(100f).row();

        // НОВЫЙ ЭЛЕМЕНТ: Выбор типа дисплея
        content.add("Тип дисплея:").colspan(2).left().row();
        Table displaySelector = new Table();
        // Получаем все доступные в игре логические дисплеи
        Seq<LogicDisplay> displays = Vars.content.blocks().select(b -> b instanceof LogicDisplay).as();
        // Создаем стандартный для Mindustry селектор блоков
        ItemSelection.buildTable(displaySelector, displays, () -> selectedDisplay, d -> {
            selectedDisplay = d; // Обновляем выбранный дисплей
        });
        content.add(displaySelector).colspan(2).left().row();

        // --- КОНЕЦ ИЗМЕНЕНИЙ ---

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
            Schematic schematic = null;
            try {
                int displaysX = Integer.parseInt(displaysXField.getText());
                int displaysY = Integer.parseInt(displaysYField.getText());

                LogicCore logic = new LogicCore();
                // ОБНОВЛЕНО: Передаем выбранный дисплей в ядро логики
                schematic = logic.processImage(imageFile, displaysX, displaysY, selectedDisplay);
            } catch (Exception e) {
                Log.err("Критическая ошибка при создании чертежа!", e);
            } finally {
                Vars.ui.loadfrag.hide();
                
                Schematic finalSchematic = schematic;
                Core.app.post(() -> {
                    if (finalSchematic != null) {
                        Vars.ui.schematics.hide();
                        Vars.control.input.useSchematic(finalSchematic);
                    } else {
                        Vars.ui.showInfo("[scarlet]Не удалось создать чертеж. Проверьте логи.[]");
                    }
                });
            }
        }).start();
    }
}