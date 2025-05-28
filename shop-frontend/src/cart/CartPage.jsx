import React from 'react';
import { useCart } from './CartContext';
import CheckoutForm from './CheckoutForm';

const CartPage = () => {
    const {
        cart,
        loading,
        error,
        updateCartItem,
        removeFromCart,
        clearCart,
        getCartTotal,
    } = useCart();

    const handleQuantityChange = (cartItemId, newQuantity) => {
        if (newQuantity < 1) return;
        updateCartItem(cartItemId, newQuantity);
    };

    if (loading) return <div className="p-4">Loading cart...</div>;
    if (error) return <div className="p-4 text-red-600">{error}</div>;
    if (cart.length === 0) return <div className="p-4">Your cart is empty</div>;

    return (
        <div className="p-6 max-w-2xl mx-auto">
            <h2 className="text-2xl font-bold mb-6">Your Cart</h2>

            {cart.map((item) => (
                <div key={item.id} className="flex items-center justify-between border-b py-4">
                    <div>
                        <h3 className="text-lg font-medium">{item.productName}</h3>
                        <p className="text-gray-600">${item.price.toFixed(2)} each</p>
                    </div>

                    <div className="flex items-center space-x-2">
                        <button
                            onClick={() => handleQuantityChange(item.id, item.quantity - 1)}
                            className="px-3 py-1 bg-gray-200 rounded-l hover:bg-gray-300"
                        >
                            -
                        </button>
                        <span className="px-3">{item.quantity}</span>
                        <button
                            onClick={() => handleQuantityChange(item.id, item.quantity + 1)}
                            className="px-3 py-1 bg-gray-200 rounded-r hover:bg-gray-300"
                        >
                            +
                        </button>

                        <button
                            onClick={() => removeFromCart(item.id)}
                            className="ml-4 text-red-600 hover:underline"
                        >
                            Remove
                        </button>
                    </div>
                </div>
            ))}

            <div className="mt-6 flex justify-between items-center">
                <button
                    onClick={clearCart}
                    className="text-red-600 hover:underline"
                >
                    Clear Cart
                </button>
                <div className="text-lg font-bold">
                    Total: ${getCartTotal().toFixed(2)}
                </div>
            </div>

            <div className="mt-10">
                <CheckoutForm />
            </div>
        </div>
    );
};

export default CartPage;
