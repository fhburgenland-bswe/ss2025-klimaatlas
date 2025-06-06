import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HealthdatawriterComponent } from './healthdatawriter.component';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

describe('HealthdatawriterComponent', () => {
  let component: HealthdatawriterComponent;
  let fixture: ComponentFixture<HealthdatawriterComponent>;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, CommonModule, FormsModule],
      providers: [{ provide: Router, useValue: routerSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(HealthdatawriterComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should send POST request and reset form on success by Risk', fakeAsync(() => {

    component.healthRiskText = 'Test data';
    component.saveRiskData();

    tick();
  
    const req = httpMock.expectOne('http://localhost:8081/save-risk.php');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect(req.request.responseType).toBe('text');

    req.flush('success');

    tick();

    expect(component.healthRiskText).toBe('');
    expect(component.showModal).toBeTrue();
  }));

  it('should log error on failed POST request by Risk', fakeAsync(() => {
    spyOn(console, 'error');
    component.healthRiskText = 'Test failure';
    component.saveRiskData();
    
    tick();

    const req = httpMock.expectOne('http://localhost:8081/save-risk.php');
    req.flush('error occurred', { status: 500, statusText: 'Server Error' });
    
    tick();

    expect(console.error).toHaveBeenCalledWith('failure', jasmine.anything());
    expect(component.showModal).toBeTrue();
  }));

  
  it('should send POST request and reset form on success by Status', fakeAsync(() => {

    component.healthStatusText = 'Test data';
    component.saveStatusData();

    tick();
  
    const req = httpMock.expectOne('http://localhost:8081/save-status.php');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect(req.request.responseType).toBe('text');

    req.flush('success');

    tick();

    expect(component.healthStatusText).toBe('');
    expect(component.showModal).toBeTrue();
  }));

  it('should log error on failed POST request by Status', fakeAsync(() => {
    spyOn(console, 'error');
    component.healthStatusText = 'Test failure';
    component.saveStatusData();
    
    tick();

    const req = httpMock.expectOne('http://localhost:8081/save-status.php');
    req.flush('error occurred', { status: 500, statusText: 'Server Error' });
    
    tick();

    expect(console.error).toHaveBeenCalledWith('failure', jasmine.anything());
    expect(component.showModal).toBeTrue();
  }));



  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should close modal', () => {
    component.showModal = true;
    component.closeModal();
    expect(component.showModal).toBeFalse();
  });

  it('should navigate home', () => {
    component.navigateHome();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['']);
  });
});
