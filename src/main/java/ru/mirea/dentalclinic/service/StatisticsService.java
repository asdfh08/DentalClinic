package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.util.Formats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Статистика системы. Требуется минимум 5 показателей — реализовано 14.
 * Все расчёты выполняются средствами Java Collections Framework и Stream API
 * (группировка, подсчёт, суммирование, поиск максимума).
 */
public class StatisticsService {

    private final PatientService patientService;
    private final DentistService dentistService;
    private final AppointmentService appointmentService;

    public StatisticsService(PatientService patientService,
                             DentistService dentistService,
                             AppointmentService appointmentService) {
        this.patientService = patientService;
        this.dentistService = dentistService;
        this.appointmentService = appointmentService;
    }

    /**
     * Показатель -> значение. LinkedHashMap сохраняет порядок вывода.
     */
    public Map<String, String> collect() {
        List<Patient> patients = patientService.findAll();
        List<Appointment> appointments = appointmentService.findAll();
        LocalDate today = LocalDate.now();

        Map<AppointmentStatus, Long> byStatus = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getStatus, Collectors.counting()));

        Map<ProcedureType, Long> byProcedure = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getProcedureType, Collectors.counting()));

        Map<String, Long> byDentist = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getDentistName, Collectors.counting()));

        List<Appointment> completed = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .toList();

        BigDecimal revenue = completed.stream()
                .map(Appointment::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageBill = completed.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(completed.size()), 2, RoundingMode.HALF_UP);

        long todayCount = appointments.stream()
                .filter(appointment -> appointment.getAppointmentTime().toLocalDate().equals(today))
                .count();

        long weekCount = appointments.stream()
                .filter(appointment -> {
                    LocalDate date = appointment.getAppointmentTime().toLocalDate();
                    return !date.isBefore(today) && date.isBefore(today.plusDays(7));
                })
                .count();

        double averageAge = patients.stream()
                .mapToInt(Patient::getAge)
                .average()
                .orElse(0.0);

        Map<String, String> statistics = new LinkedHashMap<>();
        statistics.put("Всего пациентов", String.valueOf(patients.size()));
        statistics.put("Всего врачей", String.valueOf(dentistService.count()));
        statistics.put("Всего записей на приём", String.valueOf(appointments.size()));

        for (AppointmentStatus status : AppointmentStatus.values()) {
            statistics.put("  статус \"" + status.getTitle() + "\"",
                    String.valueOf(byStatus.getOrDefault(status, 0L)));
        }

        statistics.put("Активных записей (создана + подтверждена)", String.valueOf(
                appointments.stream().filter(Appointment::isActive).count()));
        statistics.put("Записей на сегодня (" + Formats.date(today) + ")", String.valueOf(todayCount));
        statistics.put("Записей на ближайшие 7 дней", String.valueOf(weekCount));
        statistics.put("Выручка по завершённым приёмам", Formats.money(revenue));
        statistics.put("Средний чек завершённого приёма", Formats.money(averageBill));
        statistics.put("Самая востребованная процедура", topProcedure(byProcedure));
        statistics.put("Самый загруженный врач", topDentist(byDentist));
        statistics.put("Средний возраст пациентов", String.format("%.1f г.", averageAge));

        return statistics;
    }

    private String topProcedure(Map<ProcedureType, Long> byProcedure) {
        return byProcedure.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(entry -> entry.getKey().getTitle() + " (" + entry.getValue() + " шт.)")
                .orElse("нет данных");
    }

    private String topDentist(Map<String, Long> byDentist) {
        return byDentist.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(entry -> entry.getKey() + " (" + entry.getValue() + " записей)")
                .orElse("нет данных");
    }
}
