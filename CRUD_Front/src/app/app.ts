import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ItemListComponent } from './components/item-list.component'; 

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ItemListComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('CRUD_Front');
}
