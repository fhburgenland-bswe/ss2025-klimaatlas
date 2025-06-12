import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-sources',
  imports: [CommonModule],
  templateUrl: './sources.component.html',
  styleUrl: './sources.component.scss'
})
export class SourcesComponent {
  sources = [
    { url: 'https://www.openstreetmap.org/#map=8/47.184/19.509', label: 'OpenStreetMap' },
    { url: 'https://data.hub.geosphere.at/', label: 'Geosphere'},
    { url: 'https://data.hub.geosphere.at/dataset/spartacus-v2-1d-1km', label: 'Spartacus' },
    { url: 'https://api.gbif.org/', label: 'GBIF' }
  ];
}