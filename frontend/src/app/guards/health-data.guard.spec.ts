import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { healthDataGuard } from './health-data.guard';
import { AuthService } from '../services/auth.service';
import { CanActivateFn } from '@angular/router';

describe('healthDataGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let executeGuard: CanActivateFn;
  let dummyRoute: ActivatedRouteSnapshot;
  let dummyState: RouterStateSnapshot;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getFromAdmin']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    executeGuard = (...guardParameters) =>
      TestBed.runInInjectionContext(() => healthDataGuard(...guardParameters));

    dummyRoute = {} as ActivatedRouteSnapshot;
    dummyState = {} as RouterStateSnapshot;
  });

  it('should allow access when fromAdmin is true', () => {

    authServiceSpy.getFromAdmin.and.returnValue(true);

    const result = executeGuard(dummyRoute, dummyState);
    expect(result).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should deny access and navigate when fromAdmin is false', () => {
    authServiceSpy.getFromAdmin.and.returnValue(false);

    const result = executeGuard(dummyRoute, dummyState);
    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
