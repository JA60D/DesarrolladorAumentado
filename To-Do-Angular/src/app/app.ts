import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Task {
  id: number;
  title: string;
  completed: boolean;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule], // Necesario para vincular el texto del input
  template: `
    <div class="todo-container">
      <h2>📝 Mi Lista de Tareas</h2>
      
      <div class="input-group">
        <input 
          type="text" 
          [(ngModel)]="newTaskTitle" 
          (keyup.enter)="addTask()"
          placeholder="Escribe una nueva tarea..." />
        <button class="add-btn" (click)="addTask()">Añadir</button>
      </div>

      <ul>
        @for (task of tasks; track task.id) {
          <li>
            <span 
              [class.completed]="task.completed" 
              (click)="toggleTask(task)" 
              style="cursor: pointer;">
              {{ task.completed ? '✅' : '⬜' }} {{ task.title }}
            </span>
            <button class="delete-btn" (click)="deleteTask(task.id)">Eliminar</button>
          </li>
        } @empty {
          <p style="text-align: center; color: #666;">¡Felicidades, no tienes tareas pendientes! 🎉</p>
        }
      </ul>
    </div>
  `,
})
export class App {
  tasks: Task[] = [
    { id: 1, title: 'Aprender la nueva estructura de Angular', completed: true },
    { id: 2, title: 'Crear mi aplicación de tareas', completed: false }
  ];
  
  newTaskTitle = '';

  addTask() {
    if (this.newTaskTitle.trim()) {
      this.tasks.push({
        id: Date.now(),
        title: this.newTaskTitle.trim(),
        completed: false
      });
      this.newTaskTitle = '';
    }
  }

  toggleTask(task: Task) {
    task.completed = !task.completed;
  }

  deleteTask(id: number) {
    this.tasks = this.tasks.filter(t => t.id !== id);
  }
}
