// REEMPLAZA esto por tu paquete real. Ejemplo: package com.tu.paquete.principal.controller;
package com.crud.crud.controller;

import com.crud.crud.model.Item;
import com.crud.crud.repository.ItemRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
// IMPORTANTE: Permite que tu frontend de Angular (normalmente en puerto 4200) acceda a esta API
@CrossOrigin(origins = "http://localhost:4200")
public class ItemController {

    private final ItemRepository itemRepository;

    ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // Obtener todos los ítems (READ)
    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    // Obtener un ítem por ID (READ)
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear un nuevo ítem (CREATE)
    @PostMapping
    public Item createItem(@RequestBody @NonNull Item item) {
        return itemRepository.save(item);
    }

    // Actualizar un ítem existente (UPDATE)
    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @RequestBody Item itemDetails) {
        return itemRepository.findById(id)
                .map(item -> {
                    item.setName(itemDetails.getName());
                    item.setDescription(itemDetails.getDescription());
                    item.setQuantity(itemDetails.getQuantity());
                    Item updatedItem = itemRepository.save(item);
                    return ResponseEntity.ok(updatedItem);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Eliminar un ítem (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        return itemRepository.findById(id)
                .map(item -> {
                    itemRepository.delete(item);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
