import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SidePanelComponent } from './side-panel.component';
import { SourcesComponent } from './pages/sources/sources.component';
import { ContentComponent } from './pages/content/content.component';
import { HttpClientModule } from '@angular/common/http';

describe('SidePanelComponent', () => {
  let component: SidePanelComponent;
  let fixture: ComponentFixture<SidePanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidePanelComponent, SourcesComponent, ContentComponent, HttpClientModule],
    }).compileComponents();

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
});
