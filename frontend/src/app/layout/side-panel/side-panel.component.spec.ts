import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SidePanelComponent } from './side-panel.component';
import { MapService } from '../../services/map.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { City } from '../../interfaces/city-interface';
import * as L from 'leaflet';
import { of } from 'rxjs';

describe('SidePanelComponent', () => {
  let component: SidePanelComponent;
  let fixture: ComponentFixture<SidePanelComponent>;
  let mapService: jasmine.SpyObj<MapService>;
  let fakeMap: jasmine.SpyObj<L.Map>;

  const mockCities: City[] = [
    {
      place: 'Wien', zipcode: '1010', latitude: '48.2082', longitude: '16.3738',
      country_code: '', state: '', state_code: '', province: '',
      province_code: '', community: '', community_code: 'AT-9'
    },
    {
      place: 'Graz', zipcode: '8010', latitude: '47.0707', longitude: '15.4395',
      country_code: '', state: '', state_code: '', province: '',
      province_code: '', community: '', community_code: 'AT-6'
    }
  ];

  beforeEach(async () => {
    const mapSpy = jasmine.createSpyObj<L.Map>('map', ['setView', 'removeLayer', 'addLayer']);
    const mapServiceSpy = jasmine.createSpyObj<MapService>('MapService', ['getMap', 'getCities', 'loadDistricts']);

    await TestBed.configureTestingModule({
      imports: [SidePanelComponent, HttpClientTestingModule],
      providers: [
        { provide: MapService, useValue: mapServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SidePanelComponent);
    component = fixture.componentInstance;
    mapService = TestBed.inject(MapService) as jasmine.SpyObj<MapService>;
    fakeMap = mapSpy;

    mapService.getMap.and.returnValue(fakeMap);
    mapService.getCities.and.returnValue(of(mockCities));

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('onSearchChange', () => {
    it('should clear filteredCities if searchTerm is less than 2 characters', () => {
      component.searchTerm = 'w';
      component.onSearchChange();
      expect(component.filteredCities.length).toBe(0);
    });

    it('should filter cities by place or zipcode', () => {
      component.searchTerm = 'wi';
      component.onSearchChange();
      expect(component.filteredCities[0].place).toBe('Wien');

      component.searchTerm = '8010';
      component.onSearchChange();
      expect(component.filteredCities[0].place).toBe('Graz');
    });
  });

  describe('onEnterKey', () => {
    it('should select matched city by place or zipcode', () => {
      const selectSpy = spyOn(component, 'selectPlace');
      component.searchTerm = 'Graz';
      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      component.onEnterKey(event);
      expect(selectSpy).toHaveBeenCalledWith(mockCities[1]);

      component.searchTerm = '1010';
      component.onEnterKey(event);
      expect(selectSpy).toHaveBeenCalledWith(mockCities[0]);
    });

    it('should not call selectPlace if no match', () => {
      const selectSpy = spyOn(component, 'selectPlace');
      component.searchTerm = 'unknown';
      component.onEnterKey(new KeyboardEvent('keydown', { key: 'Enter' }));
      expect(selectSpy).not.toHaveBeenCalled();
    });
  });

  describe('selectPlace', () => {
    beforeEach(() => {
      // mock leaflet marker with bindPopup chain
      const fakePopup = { openPopup: jasmine.createSpy() };
      const fakeLayer = { bindPopup: () => fakePopup };
      const fakeMarker = {
        addTo: jasmine.createSpy().and.returnValue(fakeLayer),
      };
      spyOn(L, 'marker').and.returnValue(fakeMarker as unknown as L.Marker);
    });

    it('should set view, add marker, and call loadDistricts', () => {
      component.selectPlace(mockCities[0]);

      expect(fakeMap.setView)
        .toHaveBeenCalledWith([parseFloat(mockCities[0].latitude), parseFloat(mockCities[0].longitude)], 15);

      expect(mapService.loadDistricts).toHaveBeenCalledWith(mockCities[0]);
      expect(component.searchTerm).toContain('Wien');
      expect(component.filteredCities.length).toBe(0);
    });

    it('should remove previous marker if it exists', () => {
      const marker = {} as L.Marker;
      component.selectedMarker = marker;
      component.selectPlace(mockCities[1]);
      expect(fakeMap.removeLayer).toHaveBeenCalledWith(marker);
    });
  });
});
