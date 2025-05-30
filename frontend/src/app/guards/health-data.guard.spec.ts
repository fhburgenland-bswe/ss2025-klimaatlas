import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { healthDataGuard } from './health-data.guard';

describe('healthDataGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => healthDataGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
