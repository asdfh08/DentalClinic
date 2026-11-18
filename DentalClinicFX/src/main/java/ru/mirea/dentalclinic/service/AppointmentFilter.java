package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.exception.ValidationException;
import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.util.Formats;

import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Условия поиска и фильтрации записей на приём, которые пользователь задал в окне.
 *
 * Поиск идёт сразу по нескольким смысловым полям: номер записи, ФИО пациента,
 * ФИО врача, название процедуры и текст жалобы.
 * Фильтры: статус, врач, тип процедуры и период дат (любой фильтр можно не задавать — null).
 *
 * Контроллер только собирает значения из полей формы, а правило "подходит ли запись"
 * находится здесь, в слое Service. Объект используется как Predicate для FilteredList.
 */
public record AppointmentFilter(String query,
                                AppointmentStatus status,
                                Integer dentistId,
                                ProcedureType procedureType,
                                LocalDate dateFrom,
                                LocalDate dateTo) implements Predicate<Appointment> {

    /** Проверка корректности периода выполняется сразу при создании фильтра. */
    public AppointmentFilter {
        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            throw new ValidationException("период указан неверно: дата «по» (" + Formats.date(dateTo)
                    + ") раньше даты «с» (" + Formats.date(dateFrom) + "). Исправьте одну из дат");
        }
        query = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(Appointment appointment) {
        return matchesQuery(appointment)
                && (status == null || appointment.getStatus() == status)
                && (dentistId == null || appointment.getDentistId() == dentistId)
                && (procedureType == null || appointment.getProcedureType() == procedureType)
                && matchesPeriod(appointment.getAppointmentTime().toLocalDate());
    }

    private boolean matchesQuery(Appointment appointment) {
        if (query.isEmpty()) {
            return true;
        }
        return String.valueOf(appointment.getId()).equals(query)
                || contains(appointment.getPatientName())
                || contains(appointment.getDentistName())
                || contains(appointment.getProcedureType().getTitle())
                || contains(appointment.getComplaint());
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesPeriod(LocalDate date) {
        return (dateFrom == null || !date.isBefore(dateFrom))
                && (dateTo == null || !date.isAfter(dateTo));
    }
}
