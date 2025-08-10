package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.util.Log;
import mindustry.Vars;
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
            Vars.platform.showFileChooser(true, "Выбор изображения", "png", file -> {
                // ИЗМЕНЕНО: Убираем проверку file.exists()
                // Если система вернула нам объект файла, мы доверяем ему.
                if (file != null) {
                    // Добавляем больше логов для отладки
                    Log.info("File object received. Path: " + file.path());
                    Log.info("Is readable: " + file.read()); // Проверим, можно ли его читать

                    // Сразу пытаемся запустить генерацию.
                    // Вся проверка будет внутри этого метода.
                    generateSchematic(file);
                    dialog.hide();
                } else {
                    // Эта ветка сработает, если пользователь просто закрыл диалог, не выбрав файл.
                    Vars.ui.showInfo("Файл не выбран.");
                }
            });
        }).padTop(20).colspan(2).growX();

        dialog.buttons.defaults().size(150, 54);
        dialog.buttons.button("Закрыть", Icon.cancel, dialog::hide);

        dialog.show();
    }

    private static void generateSchematic(Fi imageFile) {
        // ИЗМЕНЕНО: Оборачиваем ВЕСЬ метод в try-catch
        try {
            int displaysX = Integer.parseInt(displaysXField.getText());
            int displaysY = Integer.parseInt(displaysYField.getText());

            Log.info("Начинаем генерацию для файла " + imageFile.name());
            Log.info("Размер сетки дисплеев: " + displaysX + "x" + displaysY);

            // Настоящая проверка происходит здесь.
            // Если файл нечитаемый, эта строка выбросит исключение.
            byte[] imageBytes = imageFile.readBytes();

            Log.info("Файл успешно прочитан, размер: " + imageBytes.length + " байт.");

            // Здесь будет ваш код для обработки изображения
            Vars.ui.showInfo("Файл успешно прочитан!");

        } catch (Exception e) {
            // Если что-то пошло не так (файл не удалось прочитать),
            // мы поймаем ошибку здесь и сообщим пользователю.
            Log.err("Не удалось прочитать или обработать выбранный файл!", e);
            Vars.ui.showException("Ошибка чтения файла", e);
        }
    }
}