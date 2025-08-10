package com.mkso4ka.mindustry.matrixproc;

import arc.Core;
import arc.files.Fi; // Универсальный объект файла
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.io.Platform; // Ключевой класс для доступа к системным функциям
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SchematicDialog;
import mindustry.ui.Styles;
import arc.scene.ui.TextField;

public class ModUI {

    private static TextField displaysXField;
    private static TextField displaysYField;

    public static void build() {
        try {
            SchematicDialog schematicsDialog = Vars.ui.schematics;

            schematicsDialog.buttons.button("PictureToLogic", Icon.image, () -> {
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

        // --- НАЧАЛО ИЗМЕНЕНИЙ ---

        dialog.cont.button("Выбрать изображение...", Icon.file, () -> {
            // Вот она, магия!
            // Мы просим платформу показать диалог выбора файла.
            // true - для открытия файла
            // "Выбор изображения" - заголовок окна
            // "png" - фильтр по расширению (можно добавить еще, например "jpg")
            // file -> { ... } - это функция, которая будет вызвана после выбора файла
            Platform.instance.showFileChooser(true, "Выбор изображения", "png", file -> {
                // Проверяем, что файл действительно был выбран и существует
                if (file != null && file.exists()) {
                    Log.info("Выбран файл: " + file.path());
                    Vars.ui.showInfo("Выбран файл: " + file.name());

                    // Теперь можно запустить генерацию
                    generateSchematic(file);
                    dialog.hide(); // Закрываем диалог настроек после выбора
                } else {
                    Vars.ui.showInfo("Файл не выбран.");
                }
            });
        }).padTop(20).colspan(2).growX();

        // --- КОНЕЦ ИЗМЕНЕНИЙ ---

        dialog.buttons.defaults().size(150, 54);
        dialog.buttons.button("Закрыть", Icon.cancel, dialog::hide);

        dialog.show();
    }

    /**
     * Эта функция будет принимать выбранный файл и запускать вашу логику.
     * @param imageFile Выбранный пользователем файл изображения.
     */
    private static void generateSchematic(Fi imageFile) {
        try {
            int displaysX = Integer.parseInt(displaysXField.getText());
            int displaysY = Integer.parseInt(displaysYField.getText());

            Log.info("Начинаем генерацию для файла " + imageFile.name());
            Log.info("Размер сетки дисплеев: " + displaysX + "x" + displaysY);

            // 1. Прочитать байты из файла
            byte[] imageBytes = imageFile.readBytes();

            // 2. Здесь вы будете вызывать ваш класс ImageProcessor
            //    (или как вы его назвали), который принимает байты и настройки.
            //
            //    Пример:
            //    ImageProcessor processor = new ImageProcessor(imageBytes, displaysX, displaysY);
            //    String logicCode = processor.generate();
            //
            //    Schematic schematic = Schematics.read(logicCode);
            //    Vars.ui.schematics.show(schematic); // Показываем чертеж игроку

            // Пока у нас нет процессора, просто покажем уведомление
            Vars.ui.showInfo("Генерация будет добавлена позже!");

        } catch (Exception e) {
            Log.err("Ошибка при генерации чертежа!", e);
            Vars.ui.showException("Ошибка генерации", e);
        }
    }
}