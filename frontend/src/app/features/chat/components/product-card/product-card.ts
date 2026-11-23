import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { Product } from '../../../common/model/Product';

@Component({
  selector: 'app-chat-product-card',
  standalone: true,
  templateUrl: './product-card.html',
  styleUrl: './product-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatProductCardComponent {
  @Input({ required: true }) product!: Product;
  @Input() added = false;

  @Output() addToCart = new EventEmitter<Product>();

  onAddToCart(): void {
    if (this.added) return;
    this.addToCart.emit(this.product);
  }
}