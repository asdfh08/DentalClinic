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
    private static final Pattern NAME_PATTERN = Pattern.compile("^[А-Яа-яЁёA-Za-z\\s\\-]{3,120}$");
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
        checkCabinetIsFree(dentist.getCabinet(), 0);
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
        checkCabinetIsFree(checked.getCabinet(), id);
        checkSpecializationChange(dentist, checked.getSpecialization());

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

    public long count() {
        return dentistRepository.count();
    }

    private Dentist validate(Dentist dentist) {
        if (dentist.getFullName() == null || dentist.getFullName().isBlank()) {
            throw new ValidationException("ФИО врача обязательно для заполнения");
        }
        String name = dentist.getFullName().trim().replaceAll("\\s{2,}", " ");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new ValidationException("ФИО врача должно содержать от 3 до 120 букв,"
                    + " допустимы только буквы, пробел и дефис: " + dentist.getFullName());
        }
        if (dentist.getSpecialization() == null) {
            throw new ValidationException("специализация врача обязательна для заполнения");
        }

        // телефон приводится к тому же виду, что и у пациентов: 8XXXXXXXXXX -> +7XXXXXXXXXX
        String phone = dentist.getPhone() == null ? "" : dentist.getPhone().replaceAll("[\\s()\\-]", "");
        if (phone.startsWith("8") && phone.length() == 11) {
            phone = "+7" + phone.substring(1);
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new ValidationException("телефон врача должен содержать от 10 до 15 цифр,"
                    + " например +79001234567");
        }
        if (dentist.getCabinet() < MIN_CABINET || dentist.getCabinet() > MAX_CABINET) {
            throw new ValidationException("номер кабинета должен быть в диапазоне от "
                    + MIN_CABINET + " до " + MAX_CABINET);
        }
        return new Dentist(name, phone, dentist.getSpecialization(), dentist.getCabinet());
    }

    /** БП-11: один кабинет закреплён только за одним врачом. */
    private void checkCabinetIsFree(int cabinet, int excludedDentistId) {
        dentistRepository.findAll().stream()
                .filter(other -> other.getId() != excludedDentistId)
                .filter(other -> other.getCabinet() == cabinet)
                .findFirst()
                .ifPresent(other -> {
                    throw new BusinessException("кабинет " + cabinet + " уже закреплён за врачом "
                            + other.getFullName());
                });
    }

    /**
     * БП-10: специализацию нельзя сменить, если у врача остаются активные записи
     * на процедуры, которые новая специализация выполнять не может.
     */
    private void checkSpecializationChange(Dentist dentist, Specialization target) {
        if (dentist.getSpecialization() == target) {
            return;
        }
        List<Appointment> blocking = appointmentRepository.findByDentistId(dentist.getId()).stream()
                .filter(Appointment::isActive)
                .filter(appointment -> !target.canPerform(appointment.getProcedureType()))
                .toList();

        if (!blocking.isEmpty()) {
            Appointment first = blocking.get(0);
            throw new BusinessException("нельзя сменить специализацию на \"" + target.getTitle()
                    + "\": у врача есть активные записи на процедуры другой специализации ("
                    + blocking.size() + " шт., например запись #" + first.getId() + " — "
                    + first.getProcedureType().getTitle() + ")");
        }
    }
}
