import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {
  form: FormGroup;
  isValid = true;

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {
    this.form = this.fb.group({
      username: [''],
      password: ['']
    });
  }

  ngOnInit(): void {
    this.form.get('username')?.statusChanges.subscribe(() => {
      if (this.form.get('username')?.touched) {
        this.isValid = true;
      }
    })
  }

  onSubmit() {
    const { username, password } = this.form.value;
    if (username == 'admin' && password == 'admin') {
      this.isValid = true;
      this.authService.setFromAdmin(true);
      this.router.navigate(['/healthdatawriter']);
    } else {
      this.isValid = false;
      this.form.reset();
    }
  }
}
