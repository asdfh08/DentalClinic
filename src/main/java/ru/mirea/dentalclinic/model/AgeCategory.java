package ru.mirea.dentalclinic.model;

/**
 * Возрастная категория пациента.
 * Перечисление сущности "Пациент": категория не хранится в базе данных,
 * а вычисляется по дате рождения (как и возраст), поэтому всегда актуальна.
 * Используется для фильтрации списка пациентов.
 */
public enum AgeCategory {

    CHILD("Ребёнок (до 18 лет)", 0, 17),
    ADULT("Взрослый (18-59 лет)", 18, 59),
    SENIOR("Пенсионер (60 лет и старше)", 60, Integer.MAX_VALUE);

    private final String title;
    private final int minAge;
    private final int maxAge;

    AgeCategory(String title, int minAge, int maxAge) {
        this.title = title;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public String getTitle() {
        return title;
    }

    public int getMinAge() {
        return minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public boolean includes(int age) {
        return age >= minAge && age <= maxAge;
    }

    /** Категория по возрасту в полных годах. */
    public static AgeCategory ofAge(int age) {
        for (AgeCategory category : values()) {
            if (category.includes(age)) {
                return category;
            }
        }
        return ADULT;
    }
}
