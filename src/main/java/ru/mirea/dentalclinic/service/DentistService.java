package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.exception.BusinessException;
import ru.mirea.dentalclinic.exception.EntityNotFoundException;
import ru.mirea.dentalclinic.exception.ValidationException;
import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Specialization;
import ru.mirea.dentalclinic.repository.AppointmentRepository;
import ru.mirea.dentalclinic.repository.DentistRepository;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Слой Service для врачей-стоматологов.
 */
public class DentistService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{10,15}$");
    private static final int MIN_CABINET = 1;
    private static final int MAX_CABINET = 500;

    private final DentistRepository dentistRepository;
    private final AppointmentRepository appointmentRepository;

    public DentistService(DentistRepository dentistRepository, AppointmentRepository appointmentRepository) {
        this.dentistRepository = dentistRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Dentist create(String fullName, String phone, Specialization specialization, int cabinet) {
        Dentist dentist = validate(new Dentist(fullName, phone, specialization, cabinet));
        dentistRepository.save(dentist);
        return dentist;
    }

    public List<Dentist> findAll() {
        return dentistRepository.findAll();
    }

    /** БП-1: запись нельзя создать к несуществующему врачу. */
    public Dentist getById(int id) {
        return dentistRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Врач", id));
    }

    public Dentist update(int id, String fullName, String phone, Specialization specialization, int cabinet) {
        Dentist dentist = getById(id);

        // повторно используем те же проверки, что и при создании
        Dentist checked = validate(new Dentist(fullName, phone, specialization, cabinet));
        dentist.setFullName(checked.getFullName());
        dentist.setPhone(checked.getPhone());
        dentist.setSpecialization(checked.getSpecialization());
        dentist.setCabinet(checked.getCabinet());

        dentistRepository.update(dentist);
        return dentist;
    }

    /**
     * БП-8: врача, за которым закреплены записи на приём, удалить нельзя
     * (в базе данных FOREIGN KEY ... ON DELETE RESTRICT).
     */
    public void delete(int id) {
        Dentist dentist = getById(id);
        int appointments = appointmentRepository.findByDentistId(id).size();

        if (appointments > 0) {
            throw new BusinessException("нельзя удалить врача " + dentist.getFullName()
                    + ": за ним закреплено записей на приём — " + appointments + " шт.");
        }
        dentistRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // Поиск
    // ------------------------------------------------------------------

    /** Поиск врачей по части ФИО. */
    public List<Dentist> searchByName(String part) {
        if (part == null || part.isBlank()) {
            throw new ValidationException("поисковый запрос не может быть пустым");
        }
        return dentistRepository.findByNameLike(part.trim());
    }

    // ------------------------------------------------------------------
    // Сортировка (прямая и обратная — см. enum DentistSort)
    // ------------------------------------------------------------------

    public List<Dentist> findAllSorted(DentistSort sort) {
        if (sort == null) {
            throw new ValidationException("способ сортировки не указан");
        }
        return dentistRepository.findAll().stream()
                .sorted(sort.getComparator())
                .toList();
    }

    // ------------------------------------------------------------------
    // Фильтрация
    // ------------------------------------------------------------------

    /** Фильтр по специализации (enum Specialization). */
    public List<Dentist> filterBySpecialization(Specialization specialization) {
        if (specialization == null) {
            throw new ValidationException("специализация не указана");
        }
        return dentistRepository.findAll().stream()
                .filter(dentist -> dentist.getSpecialization() == specialization)
                .sorted(DentistSort.NAME_ASC.getComparator())
                .toList();
    }

    /** Фильтр по диапазону номеров кабинетов (границы включаются). */
    public List<Dentist> filterByCabinetRange(int from, int to) {
        if (from < MIN_CABINET || to > MAX_CABINET) {
            throw new ValidationException("номера кабинетов должны быть в диапазоне от "
                    + MIN_CABINET + " до " + MAX_CABINET);
        }
        if (to < from) {
            throw new ValidationException("конечный номер кабинета (" + to
                    + ") меньше начального (" + from + ")");
        }
        return dentistRepository.findAll().stream()
                .filter(dentist -> dentist.getCabinet() >= from && dentist.getCabinet() <= to)
                .sorted(DentistSort.CABINET_ASC.getComparator())
                .toList();
    }

    /** Фильтр по занятости: только врачи, у которых есть активные (предстоящие) записи. */
    public List<Dentist> filterWithActiveAppointments() {
        return dentistRepository.findAll().stream()
                .filter(dentist -> appointmentRepository.findByDentistId(dentist.getId()).stream()
                        .anyMatch(Appointment::isActive))
                .sorted(DentistSort.NAME_ASC.getComparator())
                .toList();
    }

    public long count() {
        return dentistRepository.count();
    }

    private Dentist validate(Dentist dentist) {
        if (dentist.getFullName() == null || dentist.getFullName().trim().length() < 3) {
            throw new ValidationException("ФИО врача обязательно и должно содержать минимум 3 символа");
        }
        if (dentist.getSpecialization() == null) {
            throw new ValidationException("специализация врача обязательна для заполнения");
        }
        String normalizedPhone = dentist.getPhone() == null ? "" : dentist.getPhone().replaceAll("[\\s()\\-]", "");
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new ValidationException("телефон врача должен содержать от 10 до 15 цифр");
        }
        if (dentist.getCabinet() < MIN_CABINET || dentist.getCabinet() > MAX_CABINET) {
            throw new ValidationException("номер кабинета должен быть в диапазоне от "
                    + MIN_CABINET + " до " + MAX_CABINET);
        }
        return new Dentist(dentist.getFullName().trim(), normalizedPhone,
                dentist.getSpecialization(), dentist.getCabinet());
    }
}
