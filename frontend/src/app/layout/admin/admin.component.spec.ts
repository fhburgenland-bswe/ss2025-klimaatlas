import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AdminComponent } from './admin.component';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

describe('AdminComponent', () => {
  let component: AdminComponent;
  let fixture: ComponentFixture<AdminComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [AdminComponent, ReactiveFormsModule],
      providers: [{ provide: Router, useValue: routerSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize the form with empty username and password', () => {
    const formValue = component.form.value;
    expect(formValue.username).toBe('');
    expect(formValue.password).toBe('');
  });

  it('should update isValid to true when username field is touched', fakeAsync(() => {
    const usernameControl = component.form.get('username');
    usernameControl?.markAsTouched();
    usernameControl?.setValue('anything'); // triggers statusChanges
    tick(); // flush observable
    expect(component.isValid).toBeTrue();
  }));

  it('should navigate on correct credentials', () => {
    component.form.setValue({ username: 'admin', password: 'admin' });
    component.onSubmit();
    expect(component.isValid).toBeTrue();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/healthdatawriter']);
  });

  it('should set isValid to false and reset form on wrong credentials', () => {
    component.form.setValue({ username: 'user', password: 'wrong' });
    component.onSubmit();
    expect(component.isValid).toBeFalse();
  });
});
