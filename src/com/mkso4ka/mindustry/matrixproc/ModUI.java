package com.mkso4ka.mindustry.matrixproc;

import arc.Core;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField; // Оставляем этот импорт
import arc.scene.ui.layout.Table;
import arc.util.Log;

public class ModUI {

    private static TextField displaysXField;
    private static TextField displaysYField;

    public static void build() {
        Table menuTable = new Table();
        menuTable.setFillParent(true);
        menuTable.bottom().left();

        TextButton openDialogButton = new TextButton("PictureToLogic", Styles.defaultt);
        openDialogButton.getLabel().setWrap(false);
        
        openDialogButton.clicked(() -> {
            showSettingsDialog();
        });

        menuTable.add(openDialogButton).size(180, 50).pad(10);
        Core.scene.add(menuTable);
    }

    private static void showSettingsDialog() {
        BaseDialog dialog = new BaseDialog("Настройки PictureToLogic");

        dialog.cont.add("Дисплеев по X:").padRight(10);
        displaysXField = new TextField("1");
        // ИСПРАВЛЕНИЕ 1: Используем лямбда-выражение для проверки
        displaysXField.setValidator(text -> text.isEmpty() || text.matches("[0-9]+")); 
        dialog.cont.add(displaysXField).width(100f).row();

        dialog.cont.add("Дисплеев по Y:").padRight(10);
        displaysYField = new TextField("1");
        // ИСПРАВЛЕНИЕ 2: Используем лямбда-выражение для проверки
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