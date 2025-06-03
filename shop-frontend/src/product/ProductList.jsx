import React, {useContext, useEffect, useState} from 'react';
import {catalogApi} from "../api";
import ProductCard from "./Product";
import {useCart} from "../cart/CartContext";
import {AuthContext} from "../auth/AuthContext";

const ProductList = ({initialSearchQuery = ""}) => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [viewMode, setViewMode] = useState('popular'); // 'all', 'popular'
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [searchQuery, setSearchQuery] = useState(initialSearchQuery);
    const [inputValue, setInputValue] = useState(initialSearchQuery);
    const {addToCart} = useCart();
    const {token, userId, logout, isAdmin} = useContext(AuthContext);

    // Reset to page 0 on query/mode change
    useEffect(() => {
        setPage(0);
    }, [searchQuery, viewMode]);

    useEffect(() => {
        const fetchProducts = async () => {
            setLoading(true);
            setError(null);
            try {
                const endpoint = searchQuery.trim()
                    ? `/products/search?q=${encodeURIComponent(searchQuery)}&page=${page}`
                    : viewMode === 'popular'
                        ? `/products/popular?page=${page}`
                        : viewMode === 'foryou'
                            ? `/products/recommendations?page=${page}`
                            : `/products?page=${page}`;


                const response = await catalogApi.get(endpoint);
                setProducts(response.data.content || []);
                setTotalPages(response.data.totalPages || 1);
            } catch (err) {
                console.error('Error fetching products:', err);
                setError('Failed to fetch products');
            } finally {
                setLoading(false);
            }
        };

        fetchProducts();
    }, [searchQuery, viewMode, page]);

    const handleViewChange = (mode) => {
        if (mode === 'foryou' && !userId) {
            setViewMode('popular');
            return;
        }
        setSearchQuery(""); // Clear search
        setInputValue("");  // Reset input
        setViewMode(mode);
    };

    useEffect(() => {
        if (!userId && viewMode === 'foryou') {
            setViewMode('popular');
        }
    }, [userId, viewMode]);


    const handleSearch = (e) => {
        e.preventDefault();
        setSearchQuery(inputValue.trim());
    };

    return (
        <div className="container mx-auto px-4 py-8">
            {/* Search Bar */}
            <form onSubmit={handleSearch} className="mb-6 flex justify-center">
                <input
                    type="text"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    placeholder="Search products..."
                    className="px-4 py-2 border border-gray-300 rounded-l-lg w-full max-w-md"
                />
                <button
                    type="submit"
                    className="bg-blue-600 text-white px-4 py-2 rounded-r-lg hover:bg-blue-700"
                >
                    Search
                </button>
            </form>

            {searchQuery ? (
                <div className="mb-6">
                    <h2 className="text-xl font-semibold text-gray-700">
                        Search results for: <span className="text-blue-600">"{searchQuery}"</span>
                    </h2>
                </div>
            ) : (
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-xl font-semibold text-gray-700">
                        {viewMode === 'popular' ? 'Popular Products' : 'For You'}
                    </h2>
                    <div className="flex space-x-2">
                        <button
                            onClick={() => handleViewChange('popular')}
                            className={`px-4 py-2 rounded-lg transition ${
                                viewMode === 'popular'
                                    ? 'bg-blue-600 text-white'
                                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                            }`}
                        >
                            Popular
                        </button>
                        {userId && !isAdmin() && (
                            <button
                                onClick={() => handleViewChange('foryou')}
                                className={`px-4 py-2 rounded-lg transition ${
                                    viewMode === 'foryou'
                                        ? 'bg-blue-600 text-white'
                                        : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                                }`}
                            >
                                For You
                            </button>
                        )}

                        {/*<button*/}
                        {/*    onClick={() => handleViewChange('all')}*/}
                        {/*    className={`px-4 py-2 rounded-lg transition ${*/}
                        {/*        viewMode === 'all'*/}
                        {/*            ? 'bg-blue-600 text-white'*/}
                        {/*            : 'bg-gray-200 text-gray-700 hover:bg-gray-300'*/}
                        {/*    }`}*/}
                        {/*>*/}
                        {/*    All*/}
                        {/*</button>*/}
                    </div>
                </div>
            )}

            {/* Content */}
            {loading ? (
                <div className="flex justify-center items-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
                </div>
            ) : error ? (
                <div className="bg-red-50 border border-red-200 text-red-800 rounded-lg p-4 my-4">
                    <p className="font-medium">{error}</p>
                </div>
            ) : products.length === 0 ? (
                <div className="text-center text-gray-500">
                    No products found.
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                    {products.map((product) => (
                        <ProductCard key={product.id} product={product} addToCart={addToCart}/>
                    ))}
                </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="flex justify-center items-center mt-6 space-x-4">
                    <button
                        onClick={() => setPage(prev => Math.max(prev - 1, 0))}
                        disabled={page === 0}
                        className="px-4 py-2 rounded-xl border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm transition"
                    >
                        ← Previous
                    </button>

                    <span className="text-sm text-gray-600">
        Page <span className="font-medium text-gray-900">{page + 1}</span> of <span
                        className="font-medium text-gray-900">{totalPages}</span>
    </span>

                    <button
                        onClick={() => setPage(prev => Math.min(prev + 1, totalPages - 1))}
                        disabled={page >= totalPages - 1}
                        className="px-4 py-2 rounded-xl border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm transition"
                    >
                        Next →
                    </button>
                </div>

            )}
        </div>
    );
};

export default ProductList;
