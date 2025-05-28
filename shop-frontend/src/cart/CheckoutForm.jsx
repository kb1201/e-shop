import React, { useState } from 'react';
import { useCart } from './CartContext';

const CheckoutForm = () => {
    const { cart, createOrder, getCartTotal, loading } = useCart();
    const [orderDetails, setOrderDetails] = useState({
        shippingAddress: '',
        billingAddress: '',
        paymentMethod: 'CREDIT_CARD',
        paymentDetails: {
            cardNumber: '',
            expiryDate: '',
            cvv: '',
            cardHolderName: ''
        }
    });
    const [useSameAddress, setUseSameAddress] = useState(true);
    const [orderError, setOrderError] = useState(null);

    const handleInputChange = (e) => {
        const { name, value } = e.target;

        if (name.startsWith('payment.')) {
            const paymentField = name.split('.')[1];
            setOrderDetails(prev => ({
                ...prev,
                paymentDetails: {
                    ...prev.paymentDetails,
                    [paymentField]: value
                }
            }));
        } else {
            setOrderDetails(prev => ({
                ...prev,
                [name]: value
            }));
        }
    };

    const handleSameAddressChange = (e) => {
        const checked = e.target.checked;
        setUseSameAddress(checked);

        if (checked) {
            setOrderDetails(prev => ({
                ...prev,
                billingAddress: prev.shippingAddress
            }));
        } else {
            setOrderDetails(prev => ({
                ...prev,
                billingAddress: ''
            }));
        }
    };

    const handleShippingAddressChange = (e) => {
        const value = e.target.value;
        setOrderDetails(prev => ({
            ...prev,
            shippingAddress: value,
            ...(useSameAddress && { billingAddress: value })
        }));
    };

    const handleCheckout = async () => {
        try {
            setOrderError(null);

            // Validation
            if (!orderDetails.shippingAddress.trim()) {
                setOrderError('Shipping address is required');
                return;
            }

            if (!orderDetails.billingAddress.trim()) {
                setOrderError('Billing address is required');
                return;
            }

            if (!orderDetails.paymentDetails.cardNumber.trim()) {
                setOrderError('Card number is required');
                return;
            }

            if (!orderDetails.paymentDetails.cardHolderName.trim()) {
                setOrderError('Card holder name is required');
                return;
            }

            const order = await createOrder(orderDetails);
            if (order) {
                // Order created successfully
                alert('Order placed successfully!');
                // Redirect to order confirmation or orders page
            }
        } catch (error) {
            setOrderError(error.message);
        }
    };

    if (cart.length === 0) {
        return (
            <div className="text-center p-8">
                <h2 className="text-2xl font-semibold mb-4">Your cart is empty</h2>
                <p>Add some items to your cart before checking out.</p>
            </div>
        );
    }

    return (
        <div className="max-w-2xl mx-auto p-6">
            <h2 className="text-2xl font-bold mb-6">Checkout</h2>

            {orderError && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                    {orderError}
                    <button
                        onClick={() => setOrderError(null)}
                        className="float-right font-bold"
                    >
                        ×
                    </button>
                </div>
            )}

            {/* Order Summary */}
            <div className="bg-gray-50 p-4 rounded-lg mb-6">
                <h3 className="text-lg font-semibold mb-3">Order Summary</h3>
                {cart.map((item) => (
                    <div key={item.id} className="flex justify-between items-center py-2">
                        <span>{item.productName} × {item.quantity}</span>
                        <span>${(item.price * item.quantity).toFixed(2)}</span>
                    </div>
                ))}
                <div className="border-t pt-2 mt-2">
                    <div className="flex justify-between items-center font-semibold text-lg">
                        <span>Total:</span>
                        <span>${getCartTotal().toFixed(2)}</span>
                    </div>
                </div>
            </div>

            {/* Shipping Address */}
            <div className="mb-6">
                <label className="block mb-2 font-medium text-gray-700">
                    Shipping Address *
                </label>
                <textarea
                    name="shippingAddress"
                    value={orderDetails.shippingAddress}
                    onChange={handleShippingAddressChange}
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    rows="3"
                    placeholder="Enter your shipping address..."
                    required
                />
            </div>

            {/* Same as Shipping Address Checkbox */}
            <div className="mb-4">
                <label className="flex items-center">
                    <input
                        type="checkbox"
                        checked={useSameAddress}
                        onChange={handleSameAddressChange}
                        className="mr-2"
                    />
                    <span className="text-gray-700">Billing address same as shipping address</span>
                </label>
            </div>

            {/* Billing Address */}
            {!useSameAddress && (
                <div className="mb-6">
                    <label className="block mb-2 font-medium text-gray-700">
                        Billing Address *
                    </label>
                    <textarea
                        name="billingAddress"
                        value={orderDetails.billingAddress}
                        onChange={handleInputChange}
                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        rows="3"
                        placeholder="Enter your billing address..."
                        required
                    />
                </div>
            )}

            {/* Payment Method */}
            <div className="mb-6">
                <label className="block mb-2 font-medium text-gray-700">
                    Payment Method *
                </label>
                <select
                    name="paymentMethod"
                    value={orderDetails.paymentMethod}
                    onChange={handleInputChange}
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                    <option value="CREDIT_CARD">Credit Card</option>
                    <option value="DEBIT_CARD">Debit Card</option>
                    <option value="PAYPAL">PayPal</option>
                    <option value="BANK_TRANSFER">Bank Transfer</option>
                </select>
            </div>

            {/* Payment Details */}
            <div className="mb-6">
                <h3 className="text-lg font-semibold mb-3">Payment Details</h3>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="md:col-span-2">
                        <label className="block mb-2 font-medium text-gray-700">
                            Card Holder Name *
                        </label>
                        <input
                            type="text"
                            name="payment.cardHolderName"
                            value={orderDetails.paymentDetails.cardHolderName}
                            onChange={handleInputChange}
                            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            placeholder="John Doe"
                            required
                        />
                    </div>

                    <div className="md:col-span-2">
                        <label className="block mb-2 font-medium text-gray-700">
                            Card Number *
                        </label>
                        <input
                            type="text"
                            name="payment.cardNumber"
                            value={orderDetails.paymentDetails.cardNumber}
                            onChange={handleInputChange}
                            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            placeholder="1234 5678 9012 3456"
                            maxLength="19"
                            required
                        />
                    </div>

                    <div>
                        <label className="block mb-2 font-medium text-gray-700">
                            Expiry Date *
                        </label>
                        <input
                            type="text"
                            name="payment.expiryDate"
                            value={orderDetails.paymentDetails.expiryDate}
                            onChange={handleInputChange}
                            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            placeholder="MM/YY"
                            maxLength="5"
                        />
                    </div>

                    <div>
                        <label className="block mb-2 font-medium text-gray-700">
                            CVV *
                        </label>
                        <input
                            type="text"
                            name="payment.cvv"
                            value={orderDetails.paymentDetails.cvv}
                            onChange={handleInputChange}
                            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            placeholder="123"
                            maxLength="4"
                        />
                    </div>
                </div>
            </div>

            {/* Place Order Button */}
            <button
                onClick={handleCheckout}
                disabled={loading}
                className="w-full py-4 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
                {loading ? 'Processing...' : `Place Order - $${getCartTotal().toFixed(2)}`}
            </button>
        </div>
    );
};

export default CheckoutForm;