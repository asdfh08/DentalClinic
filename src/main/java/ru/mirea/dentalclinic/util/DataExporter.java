package ru.mirea.dentalclinic.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * ИНТЕРФЕЙС экспорта данных. Реализации: {@link ExcelExporter} и {@link CsvExporter}.
 *
 * Полиморфизм: консольное меню хранит List&lt;DataExporter&gt; и вызывает export()
 * не зная, какой именно формат за ним стоит.
 */
public interface DataExporter {

    /** Название формата для меню. */
    String formatName();

    /** Выгружает данные и возвращает список созданных файлов. */
    List<Path> export(ExportData data, Path targetDirectory) throws IOException;
}
