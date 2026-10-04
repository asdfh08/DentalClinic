package ru.mirea.dentalclinic.model;

import ru.mirea.dentalclinic.exception.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Специализация врача-стоматолога.
 *
 * Каждая специализация хранит список процедур, которые врач имеет право выполнять.
 * На этом построено бизнес-правило БП-10: пациента нельзя записать к врачу
 * на процедуру, не относящуюся к его специализации (имплантолог не лечит кариес).
 * Консультацию проводит врач любой специализации.
 *
 * Значения перечисления совпадают с CHECK-ограничением колонки dentists.specialization.
 */
public enum Specialization {

    THERAPIST("Стоматолог-терапевт",
            ProcedureType.CONSULTATION, ProcedureType.HYGIENE,
            ProcedureType.CARIES_TREATMENT, ProcedureType.ROOT_CANAL),

    SURGEON("Стоматолог-хирург",
            ProcedureType.CONSULTATION, ProcedureType.EXTRACTION),

    ORTHODONTIST("Ортодонт",
            ProcedureType.CONSULTATION, ProcedureType.BRACES),

    IMPLANTOLOGIST("Имплантолог",
            ProcedureType.CONSULTATION, ProcedureType.IMPLANTATION);

    private final String title;
    private final List<ProcedureType> procedures;

    Specialization(String title, ProcedureType... procedures) {
        this.title = title;
        this.procedures = List.of(procedures);
    }

    public String getTitle() {
        return title;
    }

    /** Процедуры, которые выполняет врач этой специализации (неизменяемый список). */
    public List<ProcedureType> getProcedures() {
        return procedures;
    }

    public boolean canPerform(ProcedureType procedureType) {
        return procedureType != null && procedures.contains(procedureType);
    }

    /** Названия процедур через запятую — для вывода в меню и в сообщениях об ошибках. */
    public String procedureTitles() {
        return procedures.stream()
                .map(ProcedureType::getTitle)
                .collect(Collectors.joining(", "));
    }

    /** Разбор значения, прочитанного из базы данных. */
    public static Specialization parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Специализация врача не может быть пустой");
        }
        String value = raw.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(specialization -> specialization.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Недопустимая специализация: " + raw + ". Допустимые значения: " + names()));
    }

    public static String names() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
