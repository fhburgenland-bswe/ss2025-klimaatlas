import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LensSelectorComponent } from './lens-selector.component';

describe('LensSelectorComponent', () => {
  let component: LensSelectorComponent;
  let fixture: ComponentFixture<LensSelectorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LensSelectorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LensSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle isOpen when toggleDropdown is called', () => {
    expect(component.isOpen).toBeFalse();
    component.toggleDropdown();
    expect(component.isOpen).toBeTrue();
    component.toggleDropdown();
    expect(component.isOpen).toBeFalse();
  });

  it('should emit selected lens and close dropdown on selectLens', () => {
    spyOn(component.lensSelected, 'emit');

    component.isOpen = true;
    component.selectLens('mosquito');

    expect(component.lensSelected.emit).toHaveBeenCalledWith('mosquito');
    expect(component.isOpen).toBeFalse();
  });

  it('should emit temperature lens and close dropdown', () => {
    spyOn(component.lensSelected, 'emit');

    component.isOpen = true;
    component.selectLens('temperature');

    expect(component.lensSelected.emit).toHaveBeenCalledWith('temperature');
    expect(component.isOpen).toBeFalse();
  });
});
