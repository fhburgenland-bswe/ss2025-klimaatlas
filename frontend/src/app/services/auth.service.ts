import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private fromAdmin = false;

  setFromAdmin(value: boolean) {
    this.fromAdmin = value;
  }

  getFromAdmin(): boolean {
    return this.fromAdmin;
  }
}
