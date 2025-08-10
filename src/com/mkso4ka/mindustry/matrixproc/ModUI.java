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

    /**
     * Этот метод теперь находит UI главного меню игры
     * и добавляет нашу кнопку прямо в него.
     */
    public static void build() {
        // ИСПРАВЛЕНИЕ: Мы больше не создаем свою таблицу.
        // Вместо этого мы находим группу UI, отвечающую за кнопки главного меню.
        // Vars.ui.menuGroup - это группа, содержащая все элементы главного меню.
        // В ней мы находим таблицу с внутренним именем "buttons".
        Table buttons = (Table) Vars.ui.menuGroup.find("buttons");

        if (buttons != null) {
            // Добавляем новую строку в существующую таблицу кнопок
            buttons.row();

            // Добавляем нашу кнопку в эту новую строку.
            // Она будет занимать всю ширину и иметь отступ сверху.
            buttons.button("PictureToLogic", Styles.defaultt, () -> {
                showSettingsDialog();
            }).growX().padTop(4);

        } else {
            // Этот лог поможет, если структура UI игры изменится в будущем
            Log.err("Could not find 'buttons' table in menuGroup!");
        }
    }

    /**
     * Этот метод создает и показывает диалоговое окно с настройками.
     * Он остается без изменений.
     */
    private static void showSettingsDialog() {
        // Создаем диалоговое окно с заголовком
        BaseDialog dialog = new BaseDialog("Настройки PictureToLogic");

        // --- Заполняем окно контентом ---

        dialog.cont.add("Дисплеев по X:").padRight(10);
        displaysXField = new TextField("1");
        // Проверка, что в поле введены только цифры
        displaysXField.setValidator(text -> text.isEmpty() || text.matches("[0-9]+"));
        dialog.cont.add(displaysXField).width(100f).row();

        dialog.cont.add("Дисплеев по Y:").padRight(10);
        displaysYField = new TextField("1");
        // Проверка, что в поле введены только цифры
        displaysYField.setValidator(text -> text.isEmpty() || text.matches("[0-9]+"));
        dialog.cont.add(displaysYField).width(100f).row();

        // Кнопка-заглушка для выбора файла
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