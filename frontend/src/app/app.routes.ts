import { Routes } from '@angular/router';
import { MainComponent } from './layout/main/main.component';
import { AdminComponent } from './layout/admin/admin.component';
import { HealthdatawriterComponent } from './layout/health-data-writer/healthdatawriter.component';
import { healthDataGuard } from './guards/health-data.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainComponent
  },
  {
    path: 'admin',
    component: AdminComponent
  },
  {
    path: 'healthdatawriter',
    component: HealthdatawriterComponent,
    canActivate: [healthDataGuard]
  }
];
