import { Component } from '@angular/core';
import { ProductService, Product } from '../product.service';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html'
})
export class ProductListComponent {
  products: Product[] = [];
  query: string = '';

  constructor(private productService: ProductService) {}

  search() {
    this.productService.searchProducts(this.query).subscribe(data => {
      this.products = data;
    });
  }
}
