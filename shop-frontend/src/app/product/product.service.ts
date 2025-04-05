import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Product {
  id: number;
  name: string;
  category: string;
  discounted_price: number;
  actual_price: number;
  discount_percentage: number;
  rating?: number;
  rating_count?: number;
  about_product?: string;
  img_link?: string;
  product_link?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private baseUrl = 'http://localhost:8000';

  constructor(private http: HttpClient) {}

  getProduct(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/products/${id}`);
  }

  searchProducts(query: string): Observable<Product[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<Product[]>(`${this.baseUrl}/products/search/`, { params });
  }

  getProductsByIds(ids: number[]): Observable<Product[]> {
    let params = new HttpParams();
    ids.forEach(id => params = params.append('ids', id));
    return this.http.get<Product[]>(`${this.baseUrl}/products/by_ids/`, { params });
  }
}
