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
  /** Tamaño del spinner en px */
  @Input() size: number = 48;

  /** Grosor del borde en px */
  @Input() thickness: number = 4;

  /** Color del spinner (cualquier color CSS válido) */
  @Input() color: string = '#3b82f6';

  /** Color de fondo del anillo */
  @Input() trackColor: string = '#e5e7eb';

  /** Texto opcional debajo del spinner */
  @Input() label?: string;

  /** Si true, muestra un overlay a pantalla completa */
  @Input() overlay: boolean = false;

  /** Velocidad de rotación (ej: '0.8s', '1.2s') */
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