package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.exception.BusinessException;
import ru.mirea.dentalclinic.exception.EntityNotFoundException;
import ru.mirea.dentalclinic.exception.ValidationException;
import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.repository.AppointmentRepository;
import ru.mirea.dentalclinic.util.Formats;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Слой Service основной сущности — записи пациента на стоматологический приём.
 *
 * Здесь собраны БИЗНЕС-ПРАВИЛА варианта:
 *  БП-1. Запись возможна только на существующего пациента и существующего врача.
 *  БП-2. Нельзя записать пациента на прошедшую дату и время.
 *  БП-3. Приём возможен только в рабочие часы клиники (09:00-20:00) и не в воскресенье,
 *        причём приём должен полностью укладываться в рабочий день.
 *  БП-4. Врач не может принимать двух пациентов одновременно (проверка пересечения
 *        интервалов с учётом длительности процедуры).
 *  БП-5. У пациента не может быть двух активных записей на один и тот же день.
 *  БП-6. Разрешены только переходы статусов CREATED -> CONFIRMED -> COMPLETED/CANCELLED/NO_SHOW.
 *  БП-7. Запись в финальном статусе (завершена/отменена/неявка) нельзя перенести или изменить,
 *        а завершённую запись нельзя удалить — это история лечения.
 *  БП-9. Стоимость не может быть отрицательной, жалоба — длиннее 255 символов.
 */
public class AppointmentService {

    private static final LocalTime CLINIC_OPEN = LocalTime.of(9, 0);
    private static final LocalTime CLINIC_CLOSE = LocalTime.of(20, 0);
    private static final int MAX_COMPLAINT_LENGTH = 255;
    private static final BigDecimal MAX_PRICE = new BigDecimal("500000.00");

    private final AppointmentRepository appointmentRepository;
    private final PatientService patientService;
    private final DentistService dentistService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientService patientService,
                              DentistService dentistService) {
        this.appointmentRepository = appointmentRepository;
        this.patientService = patientService;
        this.dentistService = dentistService;
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    /** Создание записи на приём со всеми проверками бизнес-правил. */
    public Appointment create(int patientId, int dentistId, LocalDateTime time,
                              ProcedureType procedureType, BigDecimal price, String complaint) {

        Patient patient = patientService.getById(requirePositiveId(patientId));   // БП-1
        Dentist dentist = dentistService.getById(dentistId);                       // БП-1

        if (procedureType == null) {
            throw new ValidationException("тип процедуры обязателен");
        }
        BigDecimal finalPrice = validatePrice(price, procedureType);               // БП-9
        String finalComplaint = validateComplaint(complaint);                      // БП-9

        checkTimeInFuture(time);                                                   // БП-2
        checkWorkingHours(time, procedureType);                                    // БП-3
        checkDentistIsFree(dentistId, time, procedureType, 0);                     // БП-4
        checkPatientHasNoOtherAppointment(patientId, time, 0);                     // БП-5

        Appointment appointment = new Appointment(patientId, dentistId, time,
                procedureType, finalPrice, finalComplaint);
        appointmentRepository.save(appointment);

        appointment.setPatientName(patient.getFullName());
        appointment.setDentistName(dentist.getFullName());
        return appointment;
    }

    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    public Appointment getById(int id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись на приём", id));
    }

    /**
     * Перенос приёма на другое время и/или изменение процедуры.
     * БП-7: изменять можно только незавершённую запись.
     */
    public Appointment reschedule(int id, LocalDateTime newTime, ProcedureType newProcedure, String newComplaint) {
        Appointment appointment = getById(id);

        if (appointment.getStatus().isFinal()) {
            throw new BusinessException("запись #" + id + " находится в статусе \""
                    + appointment.getStatus().getTitle() + "\" и не может быть изменена");
        }

        LocalDateTime time = newTime == null ? appointment.getAppointmentTime() : newTime;
        ProcedureType procedureType = newProcedure == null ? appointment.getProcedureType() : newProcedure;

        checkTimeInFuture(time);                                                   // БП-2
        checkWorkingHours(time, procedureType);                                    // БП-3
        checkDentistIsFree(appointment.getDentistId(), time, procedureType, id);   // БП-4
        checkPatientHasNoOtherAppointment(appointment.getPatientId(), time, id);   // БП-5

        appointment.setAppointmentTime(time);
        if (newProcedure != null) {
            appointment.setProcedureType(newProcedure);
            appointment.setPrice(newProcedure.getBasePrice());
        }
        if (newComplaint != null) {
            appointment.setComplaint(validateComplaint(newComplaint));
        }

        appointmentRepository.update(appointment);
        return appointment;
    }

    /** Изменение стоимости приёма (например, скидка или дополнительные работы). */
    public Appointment changePrice(int id, BigDecimal price) {
        Appointment appointment = getById(id);

        if (appointment.getStatus().isFinal()) {
            throw new BusinessException("нельзя изменить стоимость записи в статусе \""
                    + appointment.getStatus().getTitle() + "\"");
        }
        appointment.setPrice(validatePrice(price, appointment.getProcedureType()));
        appointmentRepository.update(appointment);
        return appointment;
    }

    /** БП-6: смена статуса только по разрешённым переходам. */
    public Appointment changeStatus(int id, AppointmentStatus target) {
        Appointment appointment = getById(id);
        AppointmentStatus current = appointment.getStatus();

        if (target == null) {
            throw new ValidationException("новый статус не указан");
        }
        if (current == target) {
            throw new BusinessException("запись #" + id + " уже находится в статусе \""
                    + current.getTitle() + "\"");
        }
        if (!current.canChangeTo(target)) {
            throw new BusinessException("запрещённый переход статуса: \"" + current.getTitle()
                    + "\" -> \"" + target.getTitle() + "\". Допустимые переходы: "
                    + describeTransitions(current));
        }

        appointment.setStatus(target);
        appointmentRepository.update(appointment);
        return appointment;
    }

    /** БП-7: завершённый приём удалять нельзя — он является частью истории лечения. */
    public void delete(int id) {
        Appointment appointment = getById(id);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("завершённую запись #" + id
                    + " удалить нельзя: это история лечения пациента");
        }
        appointmentRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // Поиск (минимум два способа — реализовано пять)
    // ------------------------------------------------------------------

    public List<Appointment> searchByPatientName(String part) {
        requireQuery(part);
        return appointmentRepository.findByPatientNameLike(part.trim());
    }

    public List<Appointment> searchByPatientPhone(String part) {
        requireQuery(part);
        return appointmentRepository.findByPatientPhoneLike(part.trim());
    }

    public List<Appointment> searchByComplaint(String part) {
        requireQuery(part);
        return appointmentRepository.findByComplaintLike(part.trim());
    }

    public List<Appointment> searchByDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("дата не указана");
        }
        return appointmentRepository.findByPeriod(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    public List<Appointment> searchByDentist(int dentistId) {
        dentistService.getById(dentistId);
        return appointmentRepository.findByDentistId(dentistId);
    }

    // ------------------------------------------------------------------
    // Фильтрация (минимум два фильтра — реализовано пять)
    // ------------------------------------------------------------------

    /** Фильтр по статусу выполняется средствами SQL (WHERE status = ?). */
    public List<Appointment> filterByStatus(AppointmentStatus status) {
        if (status == null) {
            throw new ValidationException("статус не указан");
        }
        return appointmentRepository.findByStatus(status);
    }

    /** Фильтр по типу процедуры выполняется средствами Stream API. */
    public List<Appointment> filterByProcedure(ProcedureType procedureType) {
        if (procedureType == null) {
            throw new ValidationException("тип процедуры не указан");
        }
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getProcedureType() == procedureType)
                .sorted(AppointmentSort.TIME_ASC.getComparator())
                .toList();
    }

    /** Только активные записи (Stream API + метод enum isActive()). */
    public List<Appointment> filterActive() {
        return appointmentRepository.findAll().stream()
                .filter(Appointment::isActive)
                .sorted(AppointmentSort.TIME_ASC.getComparator())
                .toList();
    }

    /** Фильтр по диапазону дат. */
    public List<Appointment> filterByPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("границы диапазона дат обязательны");
        }
        if (to.isBefore(from)) {
            throw new ValidationException("конечная дата (" + Formats.date(to)
                    + ") раньше начальной (" + Formats.date(from) + ")");
        }
        return appointmentRepository.findByPeriod(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    /** Фильтр по стоимости приёма не ниже указанной. */
    public List<Appointment> filterByMinPrice(BigDecimal minPrice) {
        if (minPrice == null || minPrice.signum() < 0) {
            throw new ValidationException("минимальная стоимость должна быть неотрицательным числом");
        }
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getPrice().compareTo(minPrice) >= 0)
                .sorted(AppointmentSort.PRICE_DESC.getComparator())
                .toList();
    }

    // ------------------------------------------------------------------
    // Сортировка (минимум два способа — см. enum AppointmentSort)
    // ------------------------------------------------------------------

    public List<Appointment> findAllSorted(AppointmentSort sort) {
        if (sort == null) {
            throw new ValidationException("способ сортировки не указан");
        }
        return appointmentRepository.findAll().stream()
                .sorted(sort.getComparator())
                .toList();
    }

    public long count() {
        return appointmentRepository.count();
    }

    // ------------------------------------------------------------------
    // Реализация бизнес-правил
    // ------------------------------------------------------------------

    /** БП-2. */
    private void checkTimeInFuture(LocalDateTime time) {
        if (time == null) {
            throw new ValidationException("дата и время приёма обязательны");
        }
        if (!time.isAfter(LocalDateTime.now())) {
            throw new BusinessException("нельзя записать пациента на прошедшее время ("
                    + Formats.dateTime(time) + ")");
        }
    }

    /** БП-3. */
    private void checkWorkingHours(LocalDateTime time, ProcedureType procedureType) {
        if (time.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessException("воскресенье — выходной день клиники, приём невозможен");
        }

        LocalTime start = time.toLocalTime();
        LocalDateTime end = time.plusMinutes(procedureType.getDurationMinutes());

        if (start.isBefore(CLINIC_OPEN)) {
            throw new BusinessException("клиника открывается в " + CLINIC_OPEN
                    + ", приём в " + Formats.time(time) + " невозможен");
        }
        if (!end.toLocalDate().equals(time.toLocalDate()) || end.toLocalTime().isAfter(CLINIC_CLOSE)) {
            throw new BusinessException("процедура \"" + procedureType.getTitle() + "\" длится "
                    + procedureType.getDurationMinutes() + " мин и закончится в "
                    + Formats.time(end) + ", а клиника работает до " + CLINIC_CLOSE);
        }
    }

    /** БП-4: врач не может принимать двух пациентов одновременно. */
    private void checkDentistIsFree(int dentistId, LocalDateTime time,
                                    ProcedureType procedureType, int excludedAppointmentId) {

        Appointment candidate = new Appointment(0, dentistId, time, procedureType, BigDecimal.ZERO, null);

        List<Appointment> nearby = appointmentRepository.findByDentistAndPeriod(
                dentistId, time.minusHours(4), time.plusHours(4));

        for (Appointment existing : nearby) {
            if (existing.getId() == excludedAppointmentId
                    || existing.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }
            if (candidate.overlaps(existing)) {
                throw new BusinessException("врач занят: на " + Formats.dateTime(existing.getAppointmentTime())
                        + " - " + Formats.time(existing.getEndTime()) + " уже назначен приём #"
                        + existing.getId() + " (" + existing.getPatientName() + ")");
            }
        }
    }

    /** БП-5: у пациента не может быть двух активных записей на один день. */
    private void checkPatientHasNoOtherAppointment(int patientId, LocalDateTime time, int excludedAppointmentId) {
        LocalDateTime dayStart = time.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Appointment> sameDay = appointmentRepository.findByPatientAndPeriod(patientId, dayStart, dayEnd)
                .stream()
                .filter(appointment -> appointment.getId() != excludedAppointmentId)
                .filter(Appointment::isActive)
                .toList();

        if (!sameDay.isEmpty()) {
            Appointment existing = sameDay.get(0);
            throw new BusinessException("у пациента уже есть активная запись на "
                    + Formats.date(time.toLocalDate()) + " в " + Formats.time(existing.getAppointmentTime())
                    + " (запись #" + existing.getId() + ")");
        }
    }

    /** БП-9. */
    private BigDecimal validatePrice(BigDecimal price, ProcedureType procedureType) {
        if (price == null) {
            return procedureType.getBasePrice();
        }
        if (price.signum() < 0) {
            throw new ValidationException("стоимость приёма не может быть отрицательной");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new ValidationException("стоимость приёма не может превышать " + Formats.money(MAX_PRICE));
        }
        return price;
    }

    /** БП-9. */
    private String validateComplaint(String complaint) {
        if (complaint == null) {
            return null;
        }
        String value = complaint.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > MAX_COMPLAINT_LENGTH) {
            throw new ValidationException("описание жалобы не должно превышать "
                    + MAX_COMPLAINT_LENGTH + " символов (получено " + value.length() + ")");
        }
        return value;
    }

    private String describeTransitions(AppointmentStatus status) {
        if (status.allowedTransitions().isEmpty()) {
            return "статус финальный, изменение невозможно";
        }
        return status.allowedTransitions().stream()
                .map(AppointmentStatus::getTitle)
                .collect(Collectors.joining(", "));
    }

    private void requireQuery(String part) {
        if (part == null || part.isBlank()) {
            throw new ValidationException("поисковый запрос не может быть пустым");
        }
    }

    private int requirePositiveId(int patientId) {
        if (patientId <= 0) {
            throw new ValidationException("ID пациента должен быть положительным числом");
        }
        return patientId;
    }
}
