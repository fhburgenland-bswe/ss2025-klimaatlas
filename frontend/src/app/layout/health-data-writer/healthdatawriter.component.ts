import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-healthdatawriter',
  imports: [CommonModule, FormsModule],
  templateUrl: './healthdatawriter.component.html',
  styleUrl: './healthdatawriter.component.scss'
})
export class HealthdatawriterComponent {
  healthRiskText = '';
  healthStatusText = '';
  showModal = false;

  constructor(private http: HttpClient, private router: Router) { };

  saveRiskData() {
    const formData = new FormData();
    formData.append('text', this.healthRiskText);

    this.http.post('http://localhost:8081/save-risk.php', formData, { responseType: 'text' })
      .subscribe({
        next: () => {
          console.log('success')
          this.healthRiskText = '';
          this.showModal = true;
        },
        error: (err) => {
          this.showModal = true;
          console.error('failure', err)
        }
      });
  }

  saveStatusData() {
    const formData = new FormData();
    formData.append('text', this.healthStatusText);

    this.http.post('http://localhost:8081/save-status.php', formData, { responseType: 'text' })
      .subscribe({
        next: () => {
          console.log('success')
          this.healthStatusText = '';
          this.showModal = true;
        },
        error: (err) => {
          this.showModal = true;
          console.error('failure', err)
        }
      });
  }

  closeModal() {
    this.showModal = false;
  }

  navigateHome() {
    this.router.navigate([''])
  }
}
