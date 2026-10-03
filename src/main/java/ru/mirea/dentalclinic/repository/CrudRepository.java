package ru.mirea.dentalclinic.repository;

import java.util.List;
import java.util.Optional;

/** Базовый набор операций CRUD для таблицы базы данных. */
public interface CrudRepository<T> {

    /** Сохраняет новый объект и возвращает ID, выданный базой данных. */
    int save(T entity);

    Optional<T> findById(int id);

    List<T> findAll();

    /** @return true, если строка найдена и обновлена */
    boolean update(T entity);

    /** @return true, если строка найдена и удалена */
    boolean deleteById(int id);
}
