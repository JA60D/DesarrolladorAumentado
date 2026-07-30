import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop'; // <-- CRUCIAL para Angular moderno
import { ItemService } from '../services/item.service';
import { Item } from '../models/item';
import { switchMap, startWith, Subject } from 'rxjs';

@Component({
  selector: 'app-item-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './item-list.component.html'
})
export class ItemListComponent {
  private itemService = inject(ItemService);

  // 1. Un canal (Subject) que emite una señal cada vez que necesitamos refrescar los datos
  private refresh$ = new Subject<void>();

  // 2. Transforma el flujo de datos HTTP directamente en un Signal reactivo de lectura
  // Se ejecutará automáticamente al iniciar y cada vez que invoquemos refresh$.next()
  itemsSignal = toSignal(
    this.refresh$.pipe(
      startWith(null), // Dispara la primera carga inmediatamente
      switchMap(() => this.itemService.getItems())
    ),
    { initialValue: [] as Item[] }
  );

  // Signals reactivos para controlar el formulario y el estado de edición
  newItem = signal<Item>({ name: '', description: '', quantity: 0 });
  isEditing = signal<boolean>(false);

  // Getter limpio para que el HTML consuma la lista de ítems de forma reactiva
  get items(): Item[] {
    return this.itemsSignal();
  }

  // Carga el ítem seleccionado en el formulario para editarlo
  editItem(item: Item): void {
    this.newItem.set({ ...item });
    this.isEditing.set(true);
  }

  // Guarda o actualiza según el estado
  saveItem(): void {
    const itemData = this.newItem();
    
    if (this.isEditing() && itemData.id) {
      this.itemService.updateItem(itemData.id, itemData).subscribe({
        next: () => {
          this.resetForm();
          this.refresh$.next(); // <-- Notifica el cambio de inmediato a la UI
        }
      });
    } else {
      this.itemService.createItem(itemData).subscribe({
        next: () => {
          this.resetForm();
          this.refresh$.next(); // <-- Notifica el cambio de inmediato a la UI
        }
      });
    }
  }

  deleteItem(id: number): void {
    if (confirm('¿Estás seguro de eliminar este ítem?')) {
      this.itemService.deleteItem(id).subscribe({
        next: () => {
          this.refresh$.next(); // <-- Al primer clic, limpia y repinta la tabla instantáneamente
        }
      });
    }
  }

  cancelEdit(): void {
    this.resetForm();
  }

  private resetForm(): void {
    this.newItem.set({ name: '', description: '', quantity: 0 });
    this.isEditing.set(false);
  }
}
