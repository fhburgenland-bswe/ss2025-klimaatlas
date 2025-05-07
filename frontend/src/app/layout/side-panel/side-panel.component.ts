import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SourcesComponent } from "./pages/sources/sources.component";
import { ContentComponent } from "./pages/content/content.component";

@Component({
  selector: 'app-side-panel',
  imports: [CommonModule, FormsModule, SourcesComponent, ContentComponent],
  templateUrl: './side-panel.component.html',
  styleUrls: ['./side-panel.component.scss'],
})
export class SidePanelComponent implements OnInit{
  isCollapsed = false;
  activePanel: 'content' | 'sources' = 'content';
  isMobile = false;

  ngOnInit(): void {
    this.checkViewport();
  }

@HostListener('window:resize')
checkViewport() {
    this.isMobile = window.innerWidth <= 768;
    if (this.isMobile) {
        document.body.style.overflow = this.isCollapsed ? 'hidden' : 'auto';
    }
}

  togglePanel() {
    this.isCollapsed = !this.isCollapsed;
  }

  showPanel(panel: 'content' | 'sources') {
    this.activePanel = panel;
  }
}
