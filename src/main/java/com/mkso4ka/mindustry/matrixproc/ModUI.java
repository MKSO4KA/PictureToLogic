package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.Styles;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Label;

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

        dialog.cont.button("Выбрать и обработать...", Icon.file, () -> {
            Vars.platform.showFileChooser(true, "Выбор изображения", "png", file -> {
                if (file != null) {
                    dialog.hide(); // Сначала прячем окно настроек
                    generateAndShowReport(file); // Затем запускаем обработку и показ отчета
                } else {
                    Vars.ui.showInfo("Файл не выбран.");
                }
            });
        }).padTop(20).colspan(2).growX();

        dialog.buttons.button("Закрыть", Icon.cancel, dialog::hide).size(150, 54);
        dialog.show();
    }

    private static void generateAndShowReport(Fi imageFile) {
        try {
            int displaysX = Integer.parseInt(displaysXField.getText());
            int displaysY = Integer.parseInt(displaysYField.getText());

            LogicCore logic = new LogicCore();
            String reportText = logic.processImage(imageFile, displaysX, displaysY);

            // Показываем новое окно с результатами
            showReportDialog(reportText);

        } catch (Exception e) {
            Log.err("Ошибка при запуске LogicCore!", e);
            Vars.ui.showException("Ошибка обработки", e);
        }
    }

    private static void showReportDialog(String reportText) {
        BaseDialog reportDialog = new BaseDialog("Отчет об обработке");
        
        Label reportLabel = new Label(reportText);
        reportLabel.setWrap(true); // Включаем перенос строк

        ScrollPane scroll = new ScrollPane(reportLabel, Styles.defaultPane);
        scroll.setFadeScrollBars(false);

        reportDialog.cont.add(scroll).grow().width(600).height(400); // Задаем размеры окна
        reportDialog.buttons.button("OK", Icon.ok, reportDialog::hide).size(120, 54);
        reportDialog.show();
    }
}