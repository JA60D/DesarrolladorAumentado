import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TriviaService } from './trivia.service';
import { HttpClient } from '@angular/common/http';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon'

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, 
    MatCardModule, 
    MatButtonModule, 
    MatProgressSpinnerModule, 
    MatIconModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  // Inject the service using Angular's modern inject() function
  private triviaService = inject(TriviaService);
  private http = inject(HttpClient); 
  // Expose the signal to the HTML template
  triviaQuestion = this.triviaService.currentQuestion;
  gameResults = this.triviaService.gameResults;

   // Create a new signal to display immediate feedback on screen
  gameFeedback = signal<string>(''); 

  // 1. Añade esta nueva señal al inicio de tu clase
  username = signal<string>('');
  tempName = signal<string>('');

  showResults = signal<boolean>(false); // Inicia en falso para no mostrar la pantalla final
  currentRound = signal<number>(1); 

  timeLeft = signal<number>(10);
  private timerIntervalId: any = null;

   ngOnInit(): void {
    this.triviaService.startTriviaStream();

    this.triviaService.getQuestionStream().subscribe(() => {
      this.showResults.set(false);
      this.gameFeedback.set('');
      this.startLocalCountdown();
    });

    this.triviaService.getResultsStream().subscribe(() => {
      // 1. Limpiamos el reloj de inmediato
      if (this.timerIntervalId) {
        clearInterval(this.timerIntervalId);
        this.timerIntervalId = null;
      }
      this.gameFeedback.set('🏁 Match complete!');
      
      // ✅ 2. LIMPIEZA CRÍTICA: Ponemos la pregunta en null para que el bloque 
      // @if (triviaQuestion()) del HTML se apague limpiamente y no busque propiedades viejas
      this.triviaService.currentQuestion.set(null); 
      
      // 3. Activamos la pantalla final
      this.showResults.set(true);
    });
  }

  startLocalCountdown(): void {
    // Limpia cualquier reloj previo para evitar que se aceleren los segundos
    if (this.timerIntervalId) {
      clearInterval(this.timerIntervalId);
      this.timerIntervalId = null;
    }

    const backendDuration = this.triviaQuestion()?.durationInSeconds || 15;
    this.timeLeft.set(backendDuration);

    // Start a fresh, single isolated countdown interval
    this.timerIntervalId = setInterval(() => {
   const currentSeconds = this.timeLeft();

    if (currentSeconds > 1) {
      // Step down safely
      this.timeLeft.set(currentSeconds - 1);
    } else {
      // WE HIT 0 SECONDS RIGHT NOW!
      this.timeLeft.set(0);
      
      // Stop this clock immediately so it doesn't loop forever
      clearInterval(this.timerIntervalId);
      this.timerIntervalId = null;

      // Capture active question ID
      const questionId = this.triviaQuestion()?.id;
      if (!questionId) return;

      console.log("Time hit 0! Requesting immediate next question from backend...");

      // Hit the Spring Boot 4 timeout endpoint
      this.http.post<any>('http://localhost:8080/api/v1/trivia/timeout', {
        username: this.username(),
        questionId: questionId,
        chosenAnswer: ''
      }).subscribe({
        next: (res) => console.log('Backend acknowledged timeout immediately.'),
        error: (err) => console.error('Failed to sync timeout with server:', err)
      });
    }
  }, 1000);
  }

    submitAnswer(selectedOption: string): void {
 // Safety check: Prevent clicks if time ran out or if user is viewing feedback alert
    if (this.timeLeft() === 0 || this.gameFeedback() !== '') return;

     const questionId = this.triviaQuestion()?.id;
    if (!questionId) return;

    // Send the answer back to the Spring Boot endpoint
    this.http.post<any>('http://localhost:8080/api/v1/trivia/submit', {
      username: this.username(),
      questionId: questionId,
      chosenAnswer: selectedOption
    }).subscribe({
      next: (response) => {
        // Response will tell us if it's correct or wrong
        this.gameFeedback.set(response.correct ? '🎉 Correct Answer!' : '❌ Wrong Answer!');
        
        // If correct, pause our local visual timer while the backend sends the next question
        if (response.correct) {
          if (this.timerIntervalId) {
            clearInterval(this.timerIntervalId);
            this.timerIntervalId = null;
          }
        }

        // Clear the feedback message after the celebration window
        setTimeout(() => this.gameFeedback.set(''), 3000);
      },
      error: (err) => console.error('Error submitting answer:', err)
    });
  }

  joinGame(name: string): void {
  if (name.trim()) {
    this.username.set(name.trim());
  }
}

  restartGame(): void {
  this.http.post('http://localhost:8080/api/v1/trivia/reset', {}).subscribe({
    next: () => {
      // Restablecemos el estado visual local en Angular
          console.log('Reset request processed by backend.');
    },
    error: (err) => console.error('Error al reiniciar la partida:', err)
  });
}

  ngOnDestroy(): void {
    this.triviaService.stopTriviaStream();
     if (this.timerIntervalId) {
      clearInterval(this.timerIntervalId);
    }
  }
}
