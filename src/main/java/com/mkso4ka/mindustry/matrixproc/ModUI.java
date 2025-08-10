package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.Styles;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;

public class ModUI {

    private static TextField displaysXField;
    private static TextField displaysYField;

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

        dialog.cont.add("Дисплеев по X:").padRight(10);
        displaysXField = new TextField("1");
        displaysXField.setValidator(text -> text.matches("[0-9]+") && Integer.parseInt(text) > 0);
        dialog.cont.add(displaysXField).width(100f).row();

        dialog.cont.add("Дисплеев по Y:").padRight(10);
        displaysYField = new TextField("1");
        displaysYField.setValidator(text -> text.matches("[0-9]+") && Integer.parseInt(text) > 0);
        dialog.cont.add(displaysYField).width(100f).row();

        dialog.cont.button("Выбрать и создать чертеж", Icon.file, () -> {
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
        // Показываем индикатор загрузки
        Vars.ui.loadfrag.show("Обработка изображения...");

        // Запускаем обработку в отдельном потоке, чтобы не "морозить" игру
        new Thread(() -> {
            try {
                int displaysX = Integer.parseInt(displaysXField.getText());
                int displaysY = Integer.parseInt(displaysYField.getText());

                LogicCore logic = new LogicCore();
                Schematic schematic = logic.processImage(imageFile, displaysX, displaysY);
                
                // Прячем индикатор загрузки
                Vars.ui.loadfrag.hide();

                // Если все прошло успешно, показываем чертеж
                if (schematic != null) {
                    Vars.ui.schematics.show(schematic);
                } else {
                    Vars.ui.showInfo("[scarlet]Не удалось создать чертеж. Проверьте логи.[]");
                }

            } catch (Exception e) {
                Vars.ui.loadfrag.hide();
                Log.err("Критическая ошибка при создании чертежа!", e);
                Vars.ui.showException("Ошибка", e);
            }
        }).start();
    }
}