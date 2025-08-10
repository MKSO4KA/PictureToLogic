package com.mkso4ka.mindustry.matrixproc;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LogicCore {

    private static final int COMMANDS_PER_PROCESSOR = 989;
    private static final int BORDER_SIZE = 8;

    /**
     * Главный метод, который запускает всю цепочку обработки.
     * @return Форматированная строка с результатами для вывода в диалоговом окне.
     */
    public String processImage(Fi imageFile, int displaysX, int displaysY) {
        StringBuilder report = new StringBuilder(); // Собираем отчет для пользователя
        try {
            report.append("[lime] --- НАЧАЛО ОБРАБОТКИ ---\n\n");
            int displaySize = 3;

            int displayPixelSize = getDisplayPixelSize(displaySize);
            report.append("[accent]1. Параметры:[]\n")
                  .append("  Видимая область: ").append(displayPixelSize).append("px\n")
                  .append("  Внутренняя рамка: ").append(BORDER_SIZE).append("px\n\n");

            int totalWidth = (displaysX * displayPixelSize) + (Math.max(0, displaysX - 1) * BORDER_SIZE * 2);
            int totalHeight = (displaysY * displayPixelSize) + (Math.max(0, displaysY - 1) * BORDER_SIZE * 2);
            report.append("[accent]2. Масштабирование:[]\n")
                  .append("  Изображение будет приведено к ").append(totalWidth).append("x").append(totalHeight).append("px\n\n");

            Pixmap masterPixmap = new Pixmap(imageFile);
            Pixmap scaledMasterPixmap = new Pixmap(totalWidth, totalHeight);
            scaledMasterPixmap.draw(masterPixmap, 0, 0, masterPixmap.width, masterPixmap.height, 0, 0, totalWidth, totalHeight);
            masterPixmap.dispose();

            DisplayMatrix displayMatrix = new DisplayMatrix();
            MatrixBlueprint blueprint = displayMatrix.placeDisplaysXxY(displaysX, displaysY, displaySize, DisplayProcessorMatrixFinal.PROCESSOR_REACH);
            report.append("[accent]3. Создание чертежа:[]\n")
                  .append("  Создан чертеж для ").append(blueprint.displayCoordinates.length).append(" дисплеев.\n\n");

            report.append("[accent]4. Анализ фрагментов:[]\n");
            int[] processorsPerDisplay = new int[blueprint.displayCoordinates.length];

            for (int i = 0; i < displaysY; i++) {
                for (int j = 0; j < displaysX; j++) {
                    int displayIndex = j * displaysY + i;

                    int sliceWidth = displayPixelSize + (j > 0 ? BORDER_SIZE : 0) + (j < displaysX - 1 ? BORDER_SIZE : 0);
                    int sliceHeight = displayPixelSize + (i > 0 ? BORDER_SIZE : 0) + (i < displaysY - 1 ? BORDER_SIZE : 0);
                    int subX = j * (displayPixelSize + BORDER_SIZE * 2) - (j > 0 ? BORDER_SIZE : 0);
                    int subY = i * (displayPixelSize + BORDER_SIZE * 2) - (i > 0 ? BORDER_SIZE : 0);

                    Pixmap finalSlice = new Pixmap(sliceWidth, sliceHeight);
                    finalSlice.draw(scaledMasterPixmap, 0, 0, subX, subY, sliceWidth, sliceHeight);

                    ImageProcessor processor = new ImageProcessor(finalSlice);
                    Map<Integer, List<Rect>> rects = processor.groupOptimal();

                    int offsetX = (j > 0) ? BORDER_SIZE : 0;
                    int offsetY = (i > 0) ? BORDER_SIZE : 0;

                    List<String> allCommands = generateCommandList(rects, displayPixelSize, offsetX, offsetY);
                    int commandCount = allCommands.size();
                    processorsPerDisplay[displayIndex] = (int) Math.ceil((double) commandCount / COMMANDS_PER_PROCESSOR);
                    
                    report.append("  [lightgray]Дисплей #").append(displayIndex).append(":[] ")
                          .append(commandCount).append(" команд -> [orange]").append(processorsPerDisplay[displayIndex]).append("[] проц.\n");

                    finalSlice.dispose();
                }
            }
            
            scaledMasterPixmap.dispose();

            report.append("\n[accent]--- ИТОГ АНАЛИЗА ---[]\n");
            report.append("Рассчитанные потребности: [yellow]").append(Arrays.toString(processorsPerDisplay)).append("[]\n\n");

            // Здесь будет логика размещения процессоров и генерации чертежа

            report.append("[lime] --- ОБРАБОТКА ЗАВЕРШЕНА УСПЕШНО --- []");

        } catch (Exception e) {
            Log.err("Критическая ошибка в LogicCore!", e);
            report.append("\n\n[scarlet]!!! ПРОИЗОШЛА КРИТИЧЕСКАЯ ОШИБКА !!![]\n\n")
                  .append(e.toString());
        }
        return report.toString();
    }

    private List<String> generateCommandList(Map<Integer, List<Rect>> rects, int displayPixelSize, int offsetX, int offsetY) {
        List<String> commands = new ArrayList<>();
        for (Map.Entry<Integer, List<Rect>> entry : rects.entrySet()) {
            List<Rect> rectList = entry.getValue();
            if (!rectList.isEmpty()) {
                commands.add(formatColorCommand(entry.getKey()));
                for (Rect rect : rectList) {
                    int correctedX = rect.x - offsetX;
                    int correctedY = rect.y - offsetY;
                    int mindustryY = displayPixelSize - correctedY - rect.h;
                    commands.add(String.format("draw rect %d %d %d %d 0 0", correctedX, mindustryY, rect.w, rect.h));
                }
            }
        }
        return commands;
    }

    private int getDisplayPixelSize(int displayBlockSize) {
        return (displayBlockSize == 3) ? 80 : 176;
    }

    private String formatColorCommand(int rgba8888) {
        int r = (rgba8888 >> 24) & 0xff;
        int g = (rgba8888 >> 16) & 0xff;
        int b = (rgba8888 >> 8) & 0xff;
        int a = (rgba8888) & 0xff;
        return String.format("draw color %d %d %d %d 0 0", r, g, b, a);
    }
}