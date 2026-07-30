package com.crud.crud.repository;

import com.crud.crud.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    // JpaRepository ya incluye todos los métodos CRUD básicos.
}