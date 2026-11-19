import { Product } from "../../common/model/Product";

export class CartProduct {
    product: Product;
    quantity: number = 0;
    totalPrice: number = 0.0;

    constructor(product: Product, quantity: number){
        this.product = product
        this.quantity = quantity
        this.updateTotal()
    }

    addQuantity(){
        this.quantity++;
         this.updateTotal();
    }

    removeQuantity(): void {
        if (this.quantity > 1) {
        this.quantity--;
        this.updateTotal();
        }
    }

    private updateTotal(): void {
        this.totalPrice = Number(this.product.price) * this.quantity;
    }
}
