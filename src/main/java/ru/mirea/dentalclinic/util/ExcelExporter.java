package ru.mirea.dentalclinic.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Экспорт данных системы в Excel (.xlsx) средствами Apache POI.
 * Основной формат экспорта по требованию контрольной работы.
 *
 * Книга содержит четыре листа: Записи на приём, Пациенты, Врачи, Статистика.
 */
public class ExcelExporter implements DataExporter {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String FONT_NAME = "Arial";

    @Override
    public String formatName() {
        return "Excel (.xlsx)";
    }

    @Override
    public List<Path> export(ExportData data, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);
        Path file = targetDirectory.resolve("dental_clinic_export_"
                + LocalDateTime.now().format(FILE_STAMP) + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            writeAppointments(workbook, data.appointments(), headerStyle, textStyle, moneyStyle);
            writePatients(workbook, data.patients(), headerStyle, textStyle);
            writeDentists(workbook, data.dentists(), headerStyle, textStyle);
            writeStatistics(workbook, data.statistics(), headerStyle, textStyle);

            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
        return List.of(file);
    }

    // ------------------------------------------------------------------
    // Листы книги
    // ------------------------------------------------------------------

    private void writeAppointments(Workbook workbook, List<Appointment> appointments,
                                   CellStyle headerStyle, CellStyle textStyle, CellStyle moneyStyle) {

        Sheet sheet = workbook.createSheet("Записи на приём");
        int[] widths = {6, 18, 26, 26, 26, 14, 16, 40};
        String[] headers = {"ID", "Дата и время", "Пациент", "Врач", "Процедура",
                "Статус", "Стоимость, руб.", "Жалоба"};
        createHeaderRow(sheet, headers, widths, headerStyle);

        int rowIndex = 1;
        for (Appointment appointment : appointments) {
            Row row = sheet.createRow(rowIndex++);
            text(row, 0, String.valueOf(appointment.getId()), textStyle);
            text(row, 1, Formats.dateTime(appointment.getAppointmentTime()), textStyle);
            text(row, 2, appointment.getPatientName(), textStyle);
            text(row, 3, appointment.getDentistName(), textStyle);
            text(row, 4, appointment.getProcedureType().getTitle(), textStyle);
            text(row, 5, appointment.getStatus().getTitle(), textStyle);

            Cell priceCell = row.createCell(6);
            priceCell.setCellValue(appointment.getPrice().doubleValue());
            priceCell.setCellStyle(moneyStyle);

            text(row, 7, appointment.getComplaint(), textStyle);
        }
        sheet.createFreezePane(0, 1);
    }

    private void writePatients(Workbook workbook, List<Patient> patients,
                               CellStyle headerStyle, CellStyle textStyle) {

        Sheet sheet = workbook.createSheet("Пациенты");
        int[] widths = {6, 30, 18, 28, 16, 8};
        String[] headers = {"ID", "ФИО", "Телефон", "E-mail", "Дата рождения", "Возраст"};
        createHeaderRow(sheet, headers, widths, headerStyle);

        int rowIndex = 1;
        for (Patient patient : patients) {
            Row row = sheet.createRow(rowIndex++);
            text(row, 0, String.valueOf(patient.getId()), textStyle);
            text(row, 1, patient.getFullName(), textStyle);
            text(row, 2, patient.getPhone(), textStyle);
            text(row, 3, patient.getEmail() == null ? "" : patient.getEmail(), textStyle);
            text(row, 4, Formats.date(patient.getBirthDate()), textStyle);
            text(row, 5, String.valueOf(patient.getAge()), textStyle);
        }
        sheet.createFreezePane(0, 1);
    }

    private void writeDentists(Workbook workbook, List<Dentist> dentists,
                               CellStyle headerStyle, CellStyle textStyle) {

        Sheet sheet = workbook.createSheet("Врачи");
        int[] widths = {6, 30, 18, 28, 10};
        String[] headers = {"ID", "ФИО", "Телефон", "Специализация", "Кабинет"};
        createHeaderRow(sheet, headers, widths, headerStyle);

        int rowIndex = 1;
        for (Dentist dentist : dentists) {
            Row row = sheet.createRow(rowIndex++);
            text(row, 0, String.valueOf(dentist.getId()), textStyle);
            text(row, 1, dentist.getFullName(), textStyle);
            text(row, 2, dentist.getPhone(), textStyle);
            text(row, 3, dentist.getSpecialization(), textStyle);
            text(row, 4, String.valueOf(dentist.getCabinet()), textStyle);
        }
        sheet.createFreezePane(0, 1);
    }

    private void writeStatistics(Workbook workbook, Map<String, String> statistics,
                                 CellStyle headerStyle, CellStyle textStyle) {

        Sheet sheet = workbook.createSheet("Статистика");
        int[] widths = {45, 30};
        String[] headers = {"Показатель", "Значение"};
        createHeaderRow(sheet, headers, widths, headerStyle);

        int rowIndex = 1;
        for (Map.Entry<String, String> entry : statistics.entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            text(row, 0, entry.getKey().trim(), textStyle);
            text(row, 1, entry.getValue(), textStyle);
        }

        Row stampRow = sheet.createRow(rowIndex + 1);
        text(stampRow, 0, "Выгрузка сформирована", textStyle);
        text(stampRow, 1, Formats.dateTime(LocalDateTime.now()), textStyle);
    }

    // ------------------------------------------------------------------
    // Оформление
    // ------------------------------------------------------------------

    private void createHeaderRow(Sheet sheet, String[] headers, int[] widths, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTextStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setWrapText(false);
        return style;
    }

    private CellStyle createMoneyStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }
}
