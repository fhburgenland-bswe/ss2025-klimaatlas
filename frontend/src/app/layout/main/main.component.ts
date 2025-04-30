import { Component } from '@angular/core';
import { MapComponent } from '../map/map.component';
import { SidePanelComponent } from '../side-panel/side-panel.component';

@Component({
  selector: 'app-main',
  imports: [MapComponent, SidePanelComponent],
  templateUrl: './main.component.html',
  styleUrl: './main.component.scss'
})
export class MainComponent {

}
