import React, {useState, useEffect} from 'react';
import axios from 'axios';
import {orderApi} from "../api";

const OrderHistory = ({userId}) => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [selectedOrder, setSelectedOrder] = useState(null);

    useEffect(() => {
        fetchOrders();
    }, [userId]);

    const fetchOrders = async () => {
        setLoading(true);
        try {
            // If userId is provided, fetch orders for that user, otherwise fetch all orders
            const endpoint = userId ? `/orders/user/${userId}` : '/orders';
            const response = await orderApi.get(endpoint);
            setOrders(response.data);
            setError(null);
        } catch (err) {
            setError('Failed to fetch orders');
            console.error('Error fetching orders:', err);
        } finally {
            setLoading(false);
        }
    };

    const fetchOrderDetails = async (orderId) => {
        setLoading(true);
        try {
            const response = await orderApi.get(`/orders/${orderId}`);
            setSelectedOrder(response.data);
            setError(null);
        } catch (err) {
            setError('Failed to fetch order details');
            console.error('Error fetching order details:', err);
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateString) => {
        const options = {year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'};
        return new Date(dateString).toLocaleDateString(undefined, options);
    };

    if (loading && !orders.length) return <div className="p-4">Loading orders...</div>;
    if (error) return <div className="p-4 text-red-600">{error}</div>;
    if (orders.length === 0) return <div className="p-4">No orders found</div>;

    return (
        <div className="p-4">
            <h2 className="text-xl font-bold mb-4">Order History</h2>

            <div className="flex">
                {/* Orders List */}
                <div className="w-1/3 pr-4">
                    {orders.map(order => (
                        <div
                            key={order.id}
                            className={`p-4 mb-2 border rounded cursor-pointer ${selectedOrder?.id === order.id ? 'bg-blue-100 border-blue-300' : ''}`}
                            onClick={() => fetchOrderDetails(order.id)}
                        >
                            <p><span className="font-medium">Order ID:</span> {order.id}</p>
                            <p><span className="font-medium">Date:</span> {formatDate(order.createdAt)}</p>
                            <p><span className="font-medium">Status:</span> {order.status}</p>
                            <p><span className="font-medium">Total:</span> ${order.totalAmount?.toFixed(2)}</p>
                        </div>
                    ))}
                </div>

                {/* Order Details */}
                <div className="w-2/3 pl-4 border-l">
                    {selectedOrder ? (
                        <div>
                            <h3 className="text-lg font-bold mb-2">Order Details - #{selectedOrder.id}</h3>
                            <p><span className="font-medium">Status:</span> {selectedOrder.status}</p>
                            <p><span className="font-medium">Date:</span> {formatDate(selectedOrder.createdAt)}</p>
                            <p><span className="font-medium">Shipping Address:</span> {selectedOrder.shippingAddress}
                            </p>

                            <h4 className="font-medium mt-4 mb-2">Items:</h4>
                            <table className="w-full">
                                <thead>
                                <tr className="bg-gray-100">
                                    <th className="text-left p-2">Product</th>
                                    <th className="text-right p-2">Price</th>
                                    <th className="text-right p-2">Quantity</th>
                                    <th className="text-right p-2">Subtotal</th>
                                </tr>
                                </thead>
                                <tbody>
                                {selectedOrder.items?.map(item => (
                                    <tr key={item.id} className="border-b">
                                        <td className="p-2">{item.productName}</td>
                                        <td className="p-2 text-right">${item.unitPrice?.toFixed(2)}</td>
                                        <td className="p-2 text-right">{item.quantity}</td>
                                        <td className="p-2 text-right">
                                            ${(item.unitPrice * item.quantity).toFixed(2)}
                                        </td>
                                    </tr>
                                ))}
                                <tr className="font-bold">
                                    <td className="p-2" colSpan="3">Total</td>
                                    <td className="p-2 text-right">
                                        ${selectedOrder.totalAmount?.toFixed(2)}
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        <div className="text-center p-8 text-gray-500">
                            Select an order to view details
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default OrderHistory;