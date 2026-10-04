package ru.mirea.dentalclinic.util;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;

import java.util.List;
import java.util.Map;

/**
 * Набор данных, выгружаемых из базы данных в файл.
 * record — компактный неизменяемый контейнер (Java 17).
 */
public record ExportData(List<Patient> patients,
                         List<Dentist> dentists,
                         List<Appointment> appointments,
                         Map<String, String> statistics) {
}
