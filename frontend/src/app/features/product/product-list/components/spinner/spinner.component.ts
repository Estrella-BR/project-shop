import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-spinner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './spinner.component.html',
  styleUrls: ['./spinner.component.css'],
})
export class SpinnerComponent {
  @Input() size: number = 48;

  @Input() thickness: number = 4;

  @Input() color: string = '#3b82f6';

  @Input() trackColor: string = '#e5e7eb';

  @Input() label?: string;

  @Input() overlay: boolean = false;

  @Input() speed: string = '0.8s';

  get spinnerStyles(): Record<string, string> {
    return {
      'width.px': this.size.toString(),
      'height.px': this.size.toString(),
      'border-width.px': this.thickness.toString(),
      'border-color': `${this.trackColor} ${this.trackColor} ${this.trackColor} ${this.color}`,
      'animation-duration': this.speed,
    };
  }
}