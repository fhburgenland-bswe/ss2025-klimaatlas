import { Routes } from '@angular/router';
import { MapComponent } from './pages/map/map.component';
import { MainComponent } from './layout/main/main.component';

export const routes: Routes = [
    {
      path: '',
      component: MainComponent,
      children: [
        {
            path: '',
            component: MapComponent
        }
        // Place for other contents e.g.: mosquito content, sources content, etc.
      ]
    }
  ];
