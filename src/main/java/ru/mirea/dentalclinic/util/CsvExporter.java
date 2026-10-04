package ru.mirea.dentalclinic.util;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Дополнительный формат экспорта — CSV.
 * Разделитель ';' и BOM в начале файла, чтобы Excel корректно открывал кириллицу.
 *
 * Второй класс, реализующий {@link DataExporter}: на этом построен полиморфизм
 * в меню экспорта (один вызов export() — разные форматы).
 */
public class CsvExporter implements DataExporter {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final char DELIMITER = ';';
    private static final String BOM = "\uFEFF";

    @Override
    public String formatName() {
        return "CSV (.csv)";
    }

    @Override
    public List<Path> export(ExportData data, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);
        String stamp = LocalDateTime.now().format(FILE_STAMP);
        List<Path> created = new ArrayList<>();

        created.add(writeAppointments(targetDirectory.resolve("appointments_" + stamp + ".csv"),
                data.appointments()));
        created.add(writePatients(targetDirectory.resolve("patients_" + stamp + ".csv"),
                data.patients()));
        created.add(writeDentists(targetDirectory.resolve("dentists_" + stamp + ".csv"),
                data.dentists()));

        return created;
    }

    private Path writeAppointments(Path file, List<Appointment> appointments) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writeLine(writer, "id", "appointment_time", "patient", "dentist",
                    "procedure", "status", "price", "complaint");

            for (Appointment appointment : appointments) {
                writeLine(writer,
                        String.valueOf(appointment.getId()),
                        Formats.dateTime(appointment.getAppointmentTime()),
                        appointment.getPatientName(),
                        appointment.getDentistName(),
                        appointment.getProcedureType().getTitle(),
                        appointment.getStatus().getTitle(),
                        appointment.getPrice().toPlainString(),
                        appointment.getComplaint());
            }
        }
        return file;
    }

    private Path writePatients(Path file, List<Patient> patients) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writeLine(writer, "id", "full_name", "phone", "email", "birth_date", "age");

            for (Patient patient : patients) {
                writeLine(writer,
                        String.valueOf(patient.getId()),
                        patient.getFullName(),
                        patient.getPhone(),
                        patient.getEmail() == null ? "" : patient.getEmail(),
                        Formats.date(patient.getBirthDate()),
                        String.valueOf(patient.getAge()));
            }
        }
        return file;
    }

    private Path writeDentists(Path file, List<Dentist> dentists) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writeLine(writer, "id", "full_name", "phone", "specialization", "cabinet");

            for (Dentist dentist : dentists) {
                writeLine(writer,
                        String.valueOf(dentist.getId()),
                        dentist.getFullName(),
                        dentist.getPhone(),
                        dentist.getSpecialization(),
                        String.valueOf(dentist.getCabinet()));
            }
        }
        return file;
    }

    private void writeLine(BufferedWriter writer, String... values) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(DELIMITER);
            }
            line.append(escape(values[i]));
        }
        writer.write(line.toString());
        writer.newLine();
    }

    /** Экранирование значений, содержащих разделитель, кавычки или перевод строки. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(DELIMITER) >= 0 || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
