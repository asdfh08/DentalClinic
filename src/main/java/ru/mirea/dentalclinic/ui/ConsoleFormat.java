package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.util.Formats;

import java.util.List;
import java.util.Map;

/**
 * Формирование текстовых таблиц для вывода в консоль.
 * Отвечает только за отображение — данных не изменяет.
 */
public final class ConsoleFormat {

    private static final String LINE = "=".repeat(118);

    private ConsoleFormat() {
    }

    public static String title(String text) {
        return LINE + System.lineSeparator()
                + "  " + text.toUpperCase() + System.lineSeparator()
                + LINE;
    }

    public static String subTitle(String text) {
        return System.lineSeparator() + "--- " + text.toUpperCase() + " ---";
    }

    public static String appointmentsTable(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return "Записей на приём не найдено.";
        }

        String format = "%-4s | %-16s | %-24s | %-22s | %-24s | %-13s | %14s%n";
        StringBuilder table = new StringBuilder();
        table.append(String.format(format, "ID", "Дата и время", "Пациент", "Врач",
                "Процедура", "Статус", "Стоимость"));
        table.append("-".repeat(130)).append(System.lineSeparator());

        for (Appointment appointment : appointments) {
            table.append(String.format(format,
                    appointment.getId(),
                    Formats.dateTime(appointment.getAppointmentTime()),
                    Formats.cut(appointment.getPatientName(), 24),
                    Formats.cut(appointment.getDentistName(), 22),
                    Formats.cut(appointment.getProcedureType().getTitle(), 24),
                    Formats.cut(appointment.getStatus().getTitle(), 13),
                    Formats.money(appointment.getPrice())));
        }
        table.append("Всего записей: ").append(appointments.size());
        return table.toString();
    }

    public static String appointmentCard(Appointment appointment) {
        StringBuilder card = new StringBuilder();
        card.append(System.lineSeparator()).append("ЗАПИСЬ НА ПРИЁМ #").append(appointment.getId())
                .append(System.lineSeparator());
        card.append("  Пациент:       ").append(appointment.getPatientName())
                .append(" (ID ").append(appointment.getPatientId()).append(')')
                .append(System.lineSeparator());
        card.append("  Врач:          ").append(appointment.getDentistName())
                .append(" (ID ").append(appointment.getDentistId()).append(')')
                .append(System.lineSeparator());
        card.append("  Начало приёма: ").append(Formats.dateTime(appointment.getAppointmentTime()))
                .append(System.lineSeparator());
        card.append("  Окончание:     ").append(Formats.time(appointment.getEndTime()))
                .append(" (").append(appointment.getProcedureType().getDurationMinutes()).append(" мин)")
                .append(System.lineSeparator());
        card.append("  Процедура:     ").append(appointment.getProcedureType().getTitle())
                .append(" [").append(appointment.getProcedureType().name()).append(']')
                .append(System.lineSeparator());
        card.append("  Статус:        ").append(appointment.getStatus().getTitle())
                .append(" [").append(appointment.getStatus().name()).append(']')
                .append(System.lineSeparator());
        card.append("  Стоимость:     ").append(Formats.money(appointment.getPrice()))
                .append(System.lineSeparator());
        card.append("  Жалоба:        ")
                .append(appointment.getComplaint().isBlank() ? "не указана" : appointment.getComplaint())
                .append(System.lineSeparator());
        card.append("  Создана:       ").append(Formats.dateTime(appointment.getCreatedAt()));
        return card.toString();
    }

    public static String patientsTable(List<Patient> patients) {
        if (patients.isEmpty()) {
            return "Пациентов не найдено.";
        }

        String format = "%-4s | %-30s | %-16s | %-28s | %-14s | %-8s%n";
        StringBuilder table = new StringBuilder();
        table.append(String.format(format, "ID", "ФИО", "Телефон", "E-mail", "Дата рожд.", "Возраст"));
        table.append("-".repeat(118)).append(System.lineSeparator());

        for (Patient patient : patients) {
            table.append(String.format(format,
                    patient.getId(),
                    Formats.cut(patient.getFullName(), 30),
                    patient.getPhone(),
                    Formats.cut(patient.getEmail() == null ? "-" : patient.getEmail(), 28),
                    Formats.date(patient.getBirthDate()),
                    patient.getAge()));
        }
        table.append("Всего пациентов: ").append(patients.size());
        return table.toString();
    }

    public static String dentistsTable(List<Dentist> dentists) {
        if (dentists.isEmpty()) {
            return "Врачей не найдено.";
        }

        String format = "%-4s | %-30s | %-16s | %-30s | %-8s%n";
        StringBuilder table = new StringBuilder();
        table.append(String.format(format, "ID", "ФИО", "Телефон", "Специализация", "Кабинет"));
        table.append("-".repeat(104)).append(System.lineSeparator());

        for (Dentist dentist : dentists) {
            table.append(String.format(format,
                    dentist.getId(),
                    Formats.cut(dentist.getFullName(), 30),
                    dentist.getPhone(),
                    Formats.cut(dentist.getSpecialization(), 30),
                    dentist.getCabinet()));
        }
        table.append("Всего врачей: ").append(dentists.size());
        return table.toString();
    }

    public static String statistics(Map<String, String> statistics) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : statistics.entrySet()) {
            result.append(String.format("%-48s %s%n", entry.getKey() + ":", entry.getValue()));
        }
        return result.toString();
    }
}
