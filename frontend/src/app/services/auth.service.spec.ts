import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return the correct value of fromAdmin', () =>{
    (service as unknown as { fromAdmin: boolean }).fromAdmin = true;
    expect(service.getFromAdmin()).toBeTrue();

    (service as unknown as { fromAdmin: boolean }).fromAdmin = false;
    expect(service.getFromAdmin()).toBeFalse();
  });
});
