import React, {useContext, useState} from 'react';
import {AuthContext} from '../auth/AuthContext';
import {useNavigate} from "react-router-dom"; // Assuming you have an AuthContext to manage authentication

const ProductCard = ({product, addToCart}) => {
    const [added, setAdded] = useState(false);
    const hasDiscount = product.discountedPrice && product.discountedPrice < product.actualPrice;
    const {token, userId, logout} = useContext(AuthContext);
    const navigate = useNavigate(); // Hook to navigate to different routes

    const handleAddToCart = () => {
        if (!token) {
            // If the user is not logged in, redirect to login page
            navigate('/login');
            return;
        }
        addToCart(product);
        setAdded(true);
    };

//     function encodeGitHubRawURL(originalURL) {
//         console.log("AAAAA")
//         // Split the URL into parts
//         const parts = originalURL.split('/');
// // Encode the last part (filename) separately
// // Reconstruct the URL with the encoded filename
//         parts[parts.length - 1] = parts[parts.length - 1]
//             .split(',')
//             .map(part => encodeURIComponent(part.trim()))
//             .join(',');
//         return parts.join('/');
//     }

    return (
        <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-xl transition-shadow duration-300">
            <div className="relative h-48 overflow-hidden">
                <img
                    src={product.imgLink}
                    alt={product.name}
                    className="w-full h-full object-cover transition-transform duration-300 hover:scale-105"
                />
                {hasDiscount && (
                    <div
                        className="absolute top-2 right-2 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded-md">
                        SALE
                    </div>
                )}
            </div>

            <div className="p-4">
                <div className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">
                    {product.category}
                </div>

                <h3 className="text-lg font-bold text-gray-900 mb-2 truncate">{product.name}</h3>

                <div className="flex items-center mb-3">
                    <div className="flex text-yellow-400">
                        {[...Array(5)].map((_, i) => (
                            <svg
                                key={i}
                                className={`w-4 h-4 ${i < Math.floor(product.rating) ? 'text-yellow-400' : 'text-gray-300'}`}
                                fill="currentColor"
                                viewBox="0 0 20 20"
                            >
                                <path
                                    d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118l-2.8-2.034c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                            </svg>
                        ))}
                    </div>
                    <span className="text-xs text-gray-500 ml-2">
                        {product.rating} ({product.ratingCount})
                    </span>
                </div>

                <div className="flex items-center justify-between mb-4">
                    <div>
                        {hasDiscount ? (
                            <div className="flex items-center">
                                <span className="text-lg font-bold text-gray-900">${product.discountedPrice}</span>
                                <span className="text-sm text-gray-500 line-through ml-2">${product.actualPrice}</span>
                            </div>
                        ) : (
                            <span className="text-lg font-bold text-gray-900">${product.actualPrice}</span>
                        )}
                    </div>
                </div>

                {/* Add to Cart Button */}
                <button
                    onClick={handleAddToCart}
                    className={`block w-full bg-blue-600 hover:bg-blue-700 text-white text-center py-2 rounded-md font-medium transition-colors duration-300 ${added ? 'bg-gray-500' : ''}`}
                    disabled={added}
                >
                    {added ? 'Added to Cart' : 'Add to Cart'}
                </button>
            </div>
        </div>
    );
};

export default ProductCard;
