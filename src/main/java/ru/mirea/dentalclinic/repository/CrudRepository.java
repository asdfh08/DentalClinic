package ru.mirea.dentalclinic.repository;

import java.util.List;
import java.util.Optional;

/**
 * ИНТЕРФЕЙС слоя доступа к данным (обязательное требование КР).
 * Описывает базовые CRUD-операции для любой сущности системы.
 *
 * Полиморфизм: PatientRepository, DentistRepository и AppointmentRepository
 * реализуют один и тот же контракт, и сервисы работают с ними одинаково.
 *
 * @param <T> тип сущности предметной модели
 */
public interface CrudRepository<T> {

    /** Сохранить новую запись и вернуть выданный базой данных ID. */
    int save(T entity);

    /** Найти запись по ID. Пустой Optional, если записи нет. */
    Optional<T> findById(int id);

    /** Все записи таблицы. */
    List<T> findAll();

    /** Обновить существующую запись. true, если строка была изменена. */
    boolean update(T entity);

    /** Удалить запись по ID. true, если строка была удалена. */
    boolean deleteById(int id);

    /** Количество записей в таблице (SELECT COUNT(*)). */
    long count();
}
