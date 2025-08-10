// src/main/java/com/mkso4ka/mindustry/matrixproc/ModUI.java

package com.mkso4ka.mindustry.matrixproc;

import arc.Core;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;

public class ModUI {

    // Переменные для хранения значений из полей ввода
    private static TextField displaysXField;
    private static TextField displaysYField;

    public static void build() {
        // Создаем таблицу для нашей кнопки в главном меню
        Table menuTable = new Table();
        menuTable.setFillParent(true); // Растягиваем на весь экран
        menuTable.bottom().left(); // Располагаем в нижнем левом углу

        // Создаем кнопку, которая будет открывать наше диалоговое окно
        TextButton openDialogButton = new TextButton("PictureToLogic", Styles.defaultt);
        openDialogButton.getLabel().setWrap(false);
        
        // Добавляем действие по нажатию
        openDialogButton.clicked(() -> {
            showSettingsDialog();
        });

        // Добавляем кнопку в таблицу и таблицу на главный экран
        menuTable.add(openDialogButton).size(180, 50).pad(10);
        Core.scene.add(menuTable);
    }

    private static void showSettingsDialog() {
        // Создаем диалоговое окно с заголовком
        BaseDialog dialog = new BaseDialog("Настройки PictureToLogic");

        // --- Заполняем окно контентом ---

        // 1. Поля для ввода количества дисплеев
        dialog.cont.add("Дисплеев по X:").padRight(10);
        displaysXField = new TextField("1"); // Значение по умолчанию
        displaysXField.setValidator(TextField.TextFieldValidator.integer); // Только целые числа
        dialog.cont.add(displaysXField).width(100f).row();

        dialog.cont.add("Дисплеев по Y:").padRight(10);
        displaysYField = new TextField("1"); // Значение по умолчанию
        displaysYField.setValidator(TextField.TextFieldValidator.integer); // Только целые числа
        dialog.cont.add(displaysYField).width(100f).row();

        // 2. Кнопка-заглушка для выбора файла
        dialog.cont.button("Выбрать изображение...", Icon.file, () -> {
            // Пока просто выводим сообщение, что функция не реализована
            Vars.ui.showInfo("Выбор файла будет добавлен позже");
        }).padTop(20).colspan(2).growX().row();


        // --- Добавляем кнопки управления внизу окна ---

        dialog.buttons.defaults().size(150, 54);
        
        // Кнопка "Сгенерировать"
        dialog.buttons.button("Сгенерировать", Icon.ok, () -> {
            // Здесь мы будем вызывать ваш основной код
            // А пока просто выведем значения в консоль
            String x = displaysXField.getText();
            String y = displaysYField.getText();
            Log.info("Нажата кнопка 'Сгенерировать'");
            Log.info("Дисплеев X: " + x);
            Log.info("Дисплеев Y: " + y);

            // Закрываем окно после нажатия
            dialog.hide();
        });

        // Кнопка "Закрыть"
        dialog.buttons.button("Закрыть", Icon.cancel, dialog::hide);

        // Показываем созданное окно
        dialog.show();
    }
}