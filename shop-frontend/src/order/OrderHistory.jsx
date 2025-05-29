import React, {useState, useEffect} from 'react';
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
            const response = await orderApi.get("orders");
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

    const getStatusColor = (status) => {
        switch (status.toLowerCase()) {
            case 'delivered':
                return 'text-green-600 bg-green-50 border-green-200';
            case 'shipped':
                return 'text-blue-600 bg-blue-50 border-blue-200';
            case 'processing':
                return 'text-yellow-600 bg-yellow-50 border-yellow-200';
            case 'cancelled':
                return 'text-red-600 bg-red-50 border-red-200';
            default:
                return 'text-gray-600 bg-gray-50 border-gray-200';
        }
    };

    const getStatusIcon = (status) => {
        switch (status.toLowerCase()) {
            case 'delivered':
                return <span className="mr-2">✅</span>;
            case 'shipped':
                return <span className="mr-2">🚚</span>;
            case 'processing':
                return <span className="mr-2">⏳</span>;
            case 'cancelled':
                return <span className="mr-2">❌</span>;
            case 'pending':
                return <span className="mr-2">🔄</span>;
            default:
                return <span className="mr-2">❓</span>; // fallback emoji
        }
    };

    if (loading && !orders.length) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-6">
                <div className="max-w-7xl mx-auto">
                    <div className="flex items-center justify-center h-64">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                        <span className="ml-3 text-lg text-gray-600">Loading your orders...</span>
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-6">
                <div className="max-w-7xl mx-auto">
                    <div className="bg-white rounded-xl shadow-lg p-8 text-center">
                        <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4"/>
                        <h2 className="text-xl font-semibold text-gray-800 mb-2">Oops! Something went wrong</h2>
                        <p className="text-red-600 mb-4">{error}</p>
                        <button
                            onClick={fetchOrders}
                            className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2 rounded-lg transition-colors"
                        >
                            Try Again
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    if (orders.length === 0) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-6">
                <div className="max-w-7xl mx-auto">
                    <div className="bg-white rounded-xl shadow-lg p-12 text-center">
                        <div className="w-12 h-12 bg-gray-200 rounded"/>
                        <h2 className="text-2xl font-semibold text-gray-800 mb-2">No Orders Yet</h2>
                        <p className="text-gray-600">When you place your first order, it will appear here.</p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-6">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">Order History</h1>
                    <p className="text-gray-600">Track and manage all your orders in one place</p>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Orders List */}
                    <div className="lg:col-span-1 space-y-4">
                        <h2 className="text-lg font-semibold text-gray-800 mb-4">Your Orders</h2>
                        {orders.map(order => (
                            <div
                                key={order.id}
                                className={`bg-white rounded-xl p-6 shadow-md hover:shadow-lg transition-all duration-200 cursor-pointer border-2 ${
                                    selectedOrder?.id === order.id
                                        ? 'border-indigo-500 ring-2 ring-indigo-200'
                                        : 'border-transparent hover:border-gray-200'
                                }`}
                                onClick={() => fetchOrderDetails(order.id)}
                            >
                                <div className="flex items-center justify-between mb-3">
                                    <span className="font-mono text-sm font-medium text-gray-800">#{order.id}</span>
                                    <div
                                        className={`flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium border ${getStatusColor(order.status)}`}>
                                        {getStatusIcon(order.status)}
                                        <span className="capitalize">{order.status}</span>
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <div className="flex items-center text-sm text-gray-600">
                                        <span className="mr-1">📅</span>
                                        {formatDate(order.createdAt)}
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <div className="flex items-center text-sm text-gray-600">
                                            <span className="mr-1">💳</span>
                                            Total
                                        </div>
                                        <span className="font-bold text-lg text-gray-900">
                      ${order.totalAmount?.toFixed(2)}
                    </span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Order Details */}
                    <div className="lg:col-span-2">
                        {selectedOrder ? (
                            <div className="bg-white rounded-xl shadow-lg p-8">
                                {/* Order Header */}
                                <div className="border-b border-gray-200 pb-6 mb-6">
                                    <div className="flex items-center justify-between mb-4">
                                        <h3 className="text-2xl font-bold text-gray-900">
                                            Order #{selectedOrder.id}
                                        </h3>
                                        <div
                                            className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium border ${getStatusColor(selectedOrder.status)}`}>
                                            {getStatusIcon(selectedOrder.status)}
                                            <span className="capitalize">{selectedOrder.status}</span>
                                        </div>
                                    </div>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        <div className="flex items-start gap-3">
                                            <span className="mr-1">📅</span>
                                            <div>
                                                <p className="text-sm font-medium text-gray-900">Order Date</p>
                                                <p className="text-sm text-gray-600">{formatDate(selectedOrder.createdAt)}</p>
                                            </div>
                                        </div>

                                        <div className="flex items-start gap-3">
                                            <span className="mr-1">💳</span>
                                            <div>
                                                <p className="text-sm font-medium text-gray-900">Total Amount</p>
                                                <p className="text-lg font-bold text-gray-900">${selectedOrder.totalAmount?.toFixed(2)}</p>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* Addresses */}
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                                    <div className="bg-gray-50 rounded-lg p-4">
                                        <div className="flex items-center gap-2 mb-2">
                                            <span className="mr-2">🚚</span>
                                            <h4 className="font-semibold text-gray-900">Shipping Address</h4>
                                        </div>
                                        <p className="text-gray-700 text-sm">{selectedOrder.shippingAddress}</p>
                                    </div>

                                    <div className="bg-gray-50 rounded-lg p-4">
                                        <div className="flex items-center gap-2 mb-2">
                                            <span className="w-5 h-5 text-gray-600 mr-1">📍</span>
                                            <h4 className="font-semibold text-gray-900">Billing Address</h4>
                                        </div>
                                        <p className="text-gray-700 text-sm">{selectedOrder.billingAddress}</p>
                                    </div>
                                </div>

                                {/* Items */}
                                <div>
                                    <h4 className="text-lg font-semibold text-gray-900 mb-4">Order Items</h4>
                                    <div className="bg-gray-50 rounded-lg overflow-hidden">
                                        <div className="overflow-x-auto">
                                            <table className="w-full">
                                                <thead className="bg-gray-100">
                                                <tr>
                                                    <th className="text-left p-4 font-semibold text-gray-900">Product</th>
                                                    <th className="text-right p-4 font-semibold text-gray-900">Price</th>
                                                    <th className="text-right p-4 font-semibold text-gray-900">Qty</th>
                                                    <th className="text-right p-4 font-semibold text-gray-900">Subtotal</th>
                                                </tr>
                                                </thead>
                                                <tbody className="divide-y divide-gray-200">
                                                {selectedOrder.items?.map(item => (
                                                    <tr key={item.id} className="hover:bg-white transition-colors">
                                                        <td className="p-4">
                                                            <div
                                                                className="font-medium text-gray-900">{item.productName}</div>
                                                        </td>
                                                        <td className="p-4 text-right text-gray-700">
                                                            ${item.unitPrice?.toFixed(2)}
                                                        </td>
                                                        <td className="p-4 text-right text-gray-700">
                                                            {item.quantity}
                                                        </td>
                                                        <td className="p-4 text-right font-medium text-gray-900">
                                                            ${(item.unitPrice * item.quantity).toFixed(2)}
                                                        </td>
                                                    </tr>
                                                ))}
                                                </tbody>
                                                <tfoot className="bg-gray-100">
                                                <tr>
                                                    <td className="p-4 font-bold text-gray-900" colSpan="3">
                                                        Total
                                                    </td>
                                                    <td className="p-4 text-right font-bold text-xl text-gray-900">
                                                        ${selectedOrder.totalAmount?.toFixed(2)}
                                                    </td>
                                                </tr>
                                                </tfoot>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="bg-white rounded-xl shadow-lg p-12 text-center">
                                <div className="w-12 h-12 bg-gray-200 rounded"/>
                                <h3 className="text-xl font-semibold text-gray-800 mb-2">Select an Order</h3>
                                <p className="text-gray-600">Choose an order from the list to view its details</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default OrderHistory;