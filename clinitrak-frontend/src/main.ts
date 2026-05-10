import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

/** Point d'entrée de l'application Angular 20. */
bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
