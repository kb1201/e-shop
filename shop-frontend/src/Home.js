import React, {useContext, useEffect, useState} from 'react';
import {AuthContext} from './auth/AuthContext';
import './index.css';
import ProductList from "./product/ProductList";
import {catalogApi} from "./api";
import {Link} from "react-router-dom";
import ProductCard from "./product/Product";
import {useCart} from "./cart/CartContext";

const Home = () => {
    const [popularProducts, setPopularProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const {addToCart} = useCart();

    useEffect(() => {
        const fetchPopularProducts = async () => {
            try {
                const response = await catalogApi.get('/products/popular?page=0&size=4');
                setPopularProducts(response.data.content || []);
            } catch (err) {
                setError('Failed to load popular products');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchPopularProducts();
    }, []);

    return (
        <div className="container mx-auto px-4 py-8">
            <section className="text-center mb-12">
                <h1 className="text-3xl font-bold text-blue-700 mb-4">Welcome to E-Shop!</h1>
                <p className="text-gray-600 text-lg">
                    Discover top-quality products, handpicked just for you.
                </p>
            </section>

            <section>
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-2xl font-semibold text-gray-800">Popular Products</h2>
                    <Link
                        to="/products"
                        className="text-blue-600 hover:underline font-medium"
                    >
                        View All Products →
                    </Link>
                </div>

                {loading ? (
                    <div className="flex justify-center h-32 items-center">
                        <div
                            className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
                    </div>
                ) : error ? (
                    <div className="bg-red-50 text-red-700 border border-red-200 p-4 rounded">
                        {error}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
                        {popularProducts.map((product) => (
                            <ProductCard key={product.id} product={product} addToCart={addToCart}/>
                        ))}
                    </div>
                )}
            </section>
        </div>
    );
};

export default Home;
