import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AdminDashboardComponent } from './admin-dashboard.component';

describe('AdminDashboardComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('renders the KPI counts returned by the summary endpoint', () => {
    const fixture = TestBed.createComponent(AdminDashboardComponent);

    httpMock
      .expectOne('/api/admin/orders/summary')
      .flush({ newCount: 4, inProgressCount: 2, doneCount: 11 });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="kpi-new"]')?.textContent).toContain('4');
    expect(el.querySelector('[data-testid="kpi-in-progress"]')?.textContent).toContain('2');
    expect(el.querySelector('[data-testid="kpi-done"]')?.textContent).toContain('11');
  });

  it('shows an error message when the summary cannot be loaded', () => {
    const fixture = TestBed.createComponent(AdminDashboardComponent);

    httpMock
      .expectOne('/api/admin/orders/summary')
      .flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Kennzahlen konnten nicht geladen werden',
    );
  });
});
