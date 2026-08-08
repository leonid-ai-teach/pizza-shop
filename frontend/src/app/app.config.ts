import {
  ApplicationConfig,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

// The UI is German (docs/Pizza_Shop_Master_Prompt.md §2), so prices need German
// number formatting (7,50 € rather than 7.50 €).
registerLocaleData(localeDe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(),
    { provide: LOCALE_ID, useValue: 'de-DE' },
  ],
};
