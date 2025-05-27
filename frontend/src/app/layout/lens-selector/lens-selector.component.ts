import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-lens-selector',
  imports: [CommonModule],
  templateUrl: './lens-selector.component.html',
  styleUrl: './lens-selector.component.scss'
})
export class LensSelectorComponent {
  isOpen = false;

  @Output() lensSelected = new EventEmitter<'mosquito' | 'temperature'>();

  toggleDropdown(): void {
    this.isOpen = !this.isOpen;
  }

  selectLens(lens: 'mosquito' | 'temperature'): void {
    this.lensSelected.emit(lens);
    this.isOpen = false;
  }
}