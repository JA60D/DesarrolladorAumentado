import { Injectable, NgZone, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

export interface TriviaQuestion {
  id: string;
  question: string;
  options: string[];
  durationInSeconds: number;
}

@Injectable({
  providedIn: 'root'
})
export class TriviaService {
  private sseUrl = 'http://localhost:8080/api/v1/trivia/stream';
  private eventSource: EventSource | null = null;

  // Angular 22 Signal to hold the current question state
  currentQuestion = signal<TriviaQuestion | null>(null);
  gameResults = signal<any[]>([]);

  private questionStream$ = new Subject<TriviaQuestion>();
  private resultsStream$ = new Subject<any[]>();

  constructor(private zone: NgZone) {}

  startTriviaStream(): void {
    if (this.eventSource) return; // Prevent duplicate connections

    this.eventSource = new EventSource(this.sseUrl);

    // Listen to the named event sent by Spring Boot 4
    this.eventSource.addEventListener('question-event', (event: MessageEvent) => {
      this.zone.run(() => {
        const questionData: TriviaQuestion = JSON.parse(event.data);
        this.currentQuestion.set(questionData); // Update the signal directly!
        this.questionStream$.next(questionData);
      });
    });

    this.eventSource.addEventListener('results-event', (event: MessageEvent) => {
      this.zone.run(() => {
        const resultsData = JSON.parse(event.data);
        this.gameResults.set(resultsData);
        this.currentQuestion.set(null);
        this.resultsStream$.next(resultsData);
      });
    });

    this.eventSource.onerror = (error) => {
      console.error('SSE Stream Error, reconnecting...', error);
    };
  }

  getQuestionStream(): Observable<TriviaQuestion> {
    return this.questionStream$.asObservable();
  }

  getResultsStream(): Observable<any[]> {
    return this.resultsStream$.asObservable();
  }

  stopTriviaStream(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
