import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Item } from '../models/item';

@Injectable({
  providedIn: 'root'
})
export class ItemService {

   private http = inject(HttpClient);

  // La URL exacta de tu controlador en Spring Boot
  private apiUrl = 'http://localhost:8080/api/items';

  // Obtener todos los ítems (READ)
  getItems(): Observable<Item[]> {
    return this.http.get<Item[]>(this.apiUrl);
  }

  // Obtener un ítem por ID (READ)
  getItemById(id: number): Observable<Item> {
    return this.http.get<Item>(`${this.apiUrl}/${id}`);
  }

  // Crear un nuevo ítem (CREATE)
  createItem(item: Item): Observable<Item> {
    return this.http.post<Item>(this.apiUrl, item);
  }

  // Actualizar un ítem existente (UPDATE)
  updateItem(id: number, item: Item): Observable<Item> {
    return this.http.put<Item>(`${this.apiUrl}/${id}`, item);
  }

  // Eliminar un ítem (DELETE)
  deleteItem(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' });
  }
}
