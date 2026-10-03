package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.exception.BusinessException;
import ru.mirea.dentalclinic.exception.EntityNotFoundException;
import ru.mirea.dentalclinic.exception.ValidationException;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.repository.AppointmentRepository;
import ru.mirea.dentalclinic.repository.PatientRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Слой Service для пациентов: проверка данных и бизнес-правила.
 * SQL-запросов здесь нет — только вызовы репозиториев.
 */
public class PatientService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{10,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[А-Яа-яЁёA-Za-z\\s\\-]{3,120}$");
    private static final int MAX_AGE = 120;

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public PatientService(PatientRepository patientRepository, AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /** Создание пациента. БП-9: данные проходят проверку до обращения к базе данных. */
    public Patient create(String fullName, String phone, String email, LocalDate birthDate) {
        String normalizedName = normalizeName(fullName);
        String normalizedPhone = normalizePhone(phone);
        String normalizedEmail = normalizeEmail(email);
        validateBirthDate(birthDate);

        Patient patient = new Patient(normalizedName, normalizedPhone, normalizedEmail, birthDate);
        patientRepository.save(patient);
        return patient;
    }

    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    /** БП-1: обращение к несуществующему пациенту приводит к понятной ошибке. */
    public Patient getById(int id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент", id));
    }

    public Patient update(int id, String fullName, String phone, String email, LocalDate birthDate) {
        Patient patient = getById(id);

        patient.setFullName(normalizeName(fullName));
        patient.setPhone(normalizePhone(phone));
        patient.setEmail(normalizeEmail(email));
        validateBirthDate(birthDate);
        patient.setBirthDate(birthDate);

        patientRepository.update(patient);
        return patient;
    }

    /**
     * БП-8: нельзя удалить пациента, у которого есть активные записи на приём.
     * Завершённая история приёмов удаляется вместе с пациентом (ON DELETE CASCADE).
     */
    public void delete(int id) {
        Patient patient = getById(id);
        long activeAppointments = appointmentRepository.countActiveByPatientId(id);

        if (activeAppointments > 0) {
            throw new BusinessException("нельзя удалить пациента " + patient.getFullName()
                    + ": у него есть активные записи на приём (" + activeAppointments + " шт.)."
                    + " Сначала отмените или завершите их");
        }
        patientRepository.deleteById(id);
    }

    /** Сколько записей всего числится за пациентом (для предупреждения перед удалением). */
    public int countAppointments(int patientId) {
        return appointmentRepository.findByPatientId(patientId).size();
    }

    /** Поиск пациентов по части ФИО. */
    public List<Patient> searchByName(String part) {
        requireSearchQuery(part);
        return patientRepository.findByNameLike(part.trim());
    }

    /** Поиск пациентов по части номера телефона. */
    public List<Patient> searchByPhone(String part) {
        requireSearchQuery(part);
        return patientRepository.findByPhoneLike(part.trim());
    }

    /** Сортировка коллекции пациентов по возрасту (Java Collections Framework + Stream API). */
    public List<Patient> findAllSortedByAge() {
        return patientRepository.findAll().stream()
                .sorted(Comparator.comparingInt((Patient patient) -> patient.getAge()).reversed())
                .toList();
    }

    public long count() {
        return patientRepository.count();
    }

    // ------------------------------------------------------------------
    // Проверки данных
    // ------------------------------------------------------------------

    private String normalizeName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("ФИО пациента обязательно для заполнения");
        }
        String value = fullName.trim().replaceAll("\\s{2,}", " ");
        if (!NAME_PATTERN.matcher(value).matches()) {
            throw new ValidationException("ФИО должно содержать от 3 до 120 букв,"
                    + " допустимы только буквы, пробел и дефис: " + fullName);
        }
        return value;
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new ValidationException("телефон пациента обязателен для заполнения");
        }
        String value = phone.replaceAll("[\\s()\\-]", "");
        if (value.startsWith("8") && value.length() == 11) {
            value = "+7" + value.substring(1);
        }
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new ValidationException("телефон должен содержать от 10 до 15 цифр,"
                    + " например +79001234567. Получено: " + phone);
        }
        return value;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String value = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new ValidationException("некорректный e-mail: " + email);
        }
        return value;
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new ValidationException("дата рождения обязательна для заполнения");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new ValidationException("дата рождения не может быть в будущем");
        }
        if (birthDate.isBefore(LocalDate.now().minusYears(MAX_AGE))) {
            throw new ValidationException("дата рождения слишком давняя (возраст больше " + MAX_AGE + " лет)");
        }
    }

    private void requireSearchQuery(String part) {
        if (part == null || part.isBlank()) {
            throw new ValidationException("поисковый запрос не может быть пустым");
        }
    }
}
