import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SidePanelComponent } from './side-panel.component';

describe('SidePanelComponent', () => {
  let component: SidePanelComponent;
  let fixture: ComponentFixture<SidePanelComponent>;

  beforeEach(async () => {
    fixture = TestBed.createComponent(SidePanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle isCollapsed', () => {
    expect(component.isCollapsed).toBeFalse();
    component.togglePanel();
    expect(component.isCollapsed).toBeTrue();
    component.togglePanel();
    expect(component.isCollapsed).toBeFalse();
  });

  it('should set activePanel correctly', () => {
    component.showPanel('sources');
    expect(component.activePanel).toBe('sources');
    component.showPanel('content');
    expect(component.activePanel).toBe('content');
  });

  it('should set body overflow to hidden when mobile and panel is collapsed', () => {
    component.isCollapsed = true;

    spyOnProperty(window, 'innerWidth').and.returnValue(500);

    component.checkViewport();

    expect(component.isMobile).toBeTrue();
    expect(document.body.style.overflow).toBe('hidden');
  });

  it('should have "sources" as the default active panel on init', () => {
    expect(component.activePanel).toBe('sources');
  })
});
