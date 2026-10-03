package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.util.Formats;

import java.util.ArrayList;
import java.util.List;

/** Форматирование таблиц и карточек для вывода в консоль. */
public final class ConsoleFormat {

    private static final int MAX_CELL = 28;
    private static final String EMPTY_LIST = "Список пуст.";

    private ConsoleFormat() {
    }

    public static String patientsTable(List<Patient> patients) {
        if (patients.isEmpty()) {
            return EMPTY_LIST;
        }
        List<String[]> rows = new ArrayList<>();
        for (Patient p : patients) {
            rows.add(new String[]{
                    String.valueOf(p.getId()), p.getFullName(), p.getPhone(),
                    p.getEmail() == null ? "-" : p.getEmail(),
                    Formats.date(p.getBirthDate()), String.valueOf(p.getAge())});
        }
        return table(new String[]{"ID", "ФИО", "Телефон", "E-mail", "Дата рожд.", "Возраст"}, rows);
    }

    public static String dentistsTable(List<Dentist> dentists) {
        if (dentists.isEmpty()) {
            return EMPTY_LIST;
        }
        List<String[]> rows = new ArrayList<>();
        for (Dentist d : dentists) {
            rows.add(new String[]{
                    String.valueOf(d.getId()), d.getFullName(), d.getPhone(),
                    d.getSpecialization(), String.valueOf(d.getCabinet())});
        }
        return table(new String[]{"ID", "ФИО", "Телефон", "Специализация", "Кабинет"}, rows);
    }

    public static String appointmentsTable(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return EMPTY_LIST;
        }
        List<String[]> rows = new ArrayList<>();
        for (Appointment a : appointments) {
            rows.add(new String[]{
                    String.valueOf(a.getId()), Formats.dateTime(a.getAppointmentTime()),
                    a.getPatientName(), a.getDentistName(),
                    a.getProcedureType().getTitle(), a.getStatus().getTitle(),
                    Formats.money(a.getPrice())});
        }
        return table(new String[]{"ID", "Дата и время", "Пациент", "Врач", "Процедура", "Статус", "Стоимость"}, rows);
    }

    public static String appointmentCard(Appointment a) {
        return "--------------------------------------------\n"
                + "Запись #" + a.getId() + "\n"
                + "Пациент:   " + a.getPatientName() + " (ID " + a.getPatientId() + ")\n"
                + "Врач:      " + a.getDentistName() + " (ID " + a.getDentistId() + ")\n"
                + "Приём:     " + Formats.dateTime(a.getAppointmentTime())
                + " - " + Formats.time(a.getEndTime()) + "\n"
                + "Процедура: " + a.getProcedureType().getTitle() + "\n"
                + "Статус:    " + a.getStatus().getTitle() + "\n"
                + "Стоимость: " + Formats.money(a.getPrice()) + "\n"
                + "Жалоба:    " + (a.getComplaint().isBlank() ? "-" : a.getComplaint()) + "\n"
                + "Создана:   " + Formats.dateTime(a.getCreatedAt()) + "\n"
                + "--------------------------------------------";
    }

    private static String table(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                row[i] = cut(row[i]);
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        StringBuilder out = new StringBuilder();
        appendRow(out, headers, widths);
        out.append("\n");
        int total = headers.length * 3 - 1;
        for (int width : widths) {
            total += width;
        }
        out.append("-".repeat(total));
        for (String[] row : rows) {
            out.append("\n");
            appendRow(out, row, widths);
        }
        return out.toString();
    }

    private static void appendRow(StringBuilder out, String[] cells, int[] widths) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.append(" | ");
            }
            out.append(cells[i]);
            out.append(" ".repeat(widths[i] - cells[i].length()));
        }
    }

    private static String cut(String value) {
        if (value == null) {
            return "-";
        }
        return value.length() <= MAX_CELL ? value : value.substring(0, MAX_CELL - 1) + "…";
    }
}
