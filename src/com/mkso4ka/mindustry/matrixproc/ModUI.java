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

    private static TextField displaysXField;
    private static TextField displaysYField;

    public static void build() {
        try {
            // Находим таблицу с кнопками в окне "Чертежи" (Schematics)
            Table schematicsButtons = Vars.ui.schematics.buttons;

            // Добавляем нашу кнопку справа от остальных
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
        displaysXField.setValidator(text -> text.isEmpty() || text.matches("[0-9]+"));
        dialog.cont.add(displaysXField).width(100f).row();

        dialog.cont.add("Дисплеев по Y:").padRight(10);
        displaysYField = new TextField("1");
        displaysYField.setValidator(text -> text.isEmpty() || text.matches("[0-9]+"));
        dialog.cont.add(displaysYField).width(100f).row();

        dialog.cont.button("Выбрать изображение...", Icon.file, () -> {
            Vars.ui.showInfo("Выбор файла будет добавлен позже");
        }).padTop(20).colspan(2).growX().row();

        dialog.buttons.defaults().size(150, 54);
        
        dialog.buttons.button("Сгенерировать", Icon.ok, () -> {
            String x = displaysXField.getText();
            String y = displaysYField.getText();
            Log.info("Нажата кнопка 'Сгенерировать'");
            Log.info("Дисплеев X: " + x);
            Log.info("Дисплеев Y: " + y);
            dialog.hide();
        });

        dialog.buttons.button("Закрыть", Icon.cancel, dialog::hide);

        dialog.show();
    }
}