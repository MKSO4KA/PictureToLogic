package com.mkso4ka.mindustry.matrixproc;

import arc.Core; // ИСПОЛЬЗУЕМ ЭТОТ КЛАСС
import arc.files.Fi;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
// SchematicDialog не нужен, так как мы обращаемся к UI напрямую
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.Styles;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table; // Импортируем Table для работы с UI

public class ModUI {

    private static TextField displaysXField;
    private static TextField displaysYField;

    public static void build() {
        try {
            // В v147 мы обращаемся к UI немного иначе.
            // Vars.ui.schematics - это уже готовый объект, нам не нужно объявлять его тип.
            Table schematicsButtons = Vars.ui.schematics.buttons;

            schematicsButtons.button("PictureToLogic", Icon.image, () -> {
                showSettingsDialog();
            }).size(180, 64).padLeft(6);

            Log.info("PictureToLogic button added to schematics dialog.");

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

        dialog.cont.button("Выбрать изображение...", Icon.file, () -> {
            // ИЗМЕНЕНО: Вызываем файловый диалог через Core.platform
            Core.platform.showFileChooser(true, "Выбор изображения", "png", file -> {
                if (file != null && file.exists()) {
                    Log.info("Выбран файл: " + file.path());
                    Vars.ui.showInfo("Выбран файл: " + file.name());

                    generateSchematic(file);
                    dialog.hide();
                } else {
                    Vars.ui.showInfo("Файл не выбран.");
                }
            });
        }).padTop(20).colspan(2).growX();

        dialog.buttons.defaults().size(150, 54);
        dialog.buttons.button("Закрыть", Icon.cancel, dialog::hide);

        dialog.show();
    }

    private static void generateSchematic(Fi imageFile) {
        try {
            int displaysX = Integer.parseInt(displaysXField.getText());
            int displaysY = Integer.parseInt(displaysYField.getText());

            Log.info("Начинаем генерацию для файла " + imageFile.name());
            Log.info("Размер сетки дисплеев: " + displaysX + "x" + displaysY);

            byte[] imageBytes = imageFile.readBytes();

            // Здесь будет ваш код
            Vars.ui.showInfo("Генерация будет добавлена позже!");

        } catch (Exception e) {
            Log.err("Ошибка при генерации чертежа!", e);
            Vars.ui.showException("Ошибка генерации", e);
        }
    }
}