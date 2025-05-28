import React from 'react';
import { useCart } from './CartContext';
import {orderApi} from "../api";
const OrderPage = () => {
    const { cart } = useCart();

    const calculateTotal = () => {
        return cart.reduce((total, product) => {
            const price = product.actualPrice
            return total + price * (product.quantity || 1);
        }, 0).toFixed(2);
    };

    const confirmOrder = async () => {
        if (cart.length === 0) {
            alert('Your cart is empty');
            return;
        }

        // Prepare order request body - adapt as per your OrderRequest DTO
        const orderRequest = {
            items: cart.map(product => ({
                productId: product.id,
                quantity: product.quantity || 1,
            })),
            // Add other fields like user info, shipping address if needed
        };

        try {
            const response = await orderApi.post('/orders',JSON.stringify(orderRequest),);


            alert('Order placed successfully! Order ID: ' + createdOrder.id);

            //clearCart(); // Clear cart after order placed
            // Optionally navigate to order confirmation page or order details
        } catch (error) {
            console.error(error);
            alert('Error placing order: ' + error.message);
        }
    };

    return (
        <div className="container mx-auto px-4 py-8">
            <h1 className="text-2xl font-semibold mb-4">Order Summary</h1>

            {cart.length === 0 ? (
                <p>Your cart is empty</p>
            ) : (
                <>
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 mb-8">
                        {cart.map((product, index) => (
                            <div key={index} className="bg-white rounded-lg shadow-md overflow-hidden">
                                <img src={product.imgLink} alt={product.name} className="w-full h-48 object-cover" />
                                <div className="p-4">
                                    <h3 className="text-lg font-bold text-gray-900 mb-2 truncate">{product.name}</h3>
                                    <p className="text-sm text-gray-600">Quantity: {product.quantity || 1}</p>
                                    <span className="text-lg font-bold text-gray-900">
                                        ${((product.discountedPrice || product.actualPrice) * (product.quantity || 1)).toFixed(2)}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="bg-gray-100 p-6 rounded-lg shadow-md w-full max-w-md mx-auto">
                        <h2 className="text-xl font-semibold mb-4">Total</h2>
                        <div className="flex justify-between items-center mb-4">
                            <span className="text-lg">Total Price:</span>
                            <span className="text-lg font-bold">${calculateTotal()}</span>
                        </div>
                        <button
                            onClick={confirmOrder}
                            className="w-full bg-green-600 text-white py-2 px-4 rounded hover:bg-green-700 transition"
                        >
                            Confirm Order
                        </button>
                    </div>
                </>
            )}
        </div>
    );
};

export default OrderPage;
