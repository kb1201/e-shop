import React, {useState, useEffect} from 'react';
import {shipmentApi} from "../api";

const ShipmentAdmin = () => {
    const [shipments, setShipments] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [selectedShipment, setSelectedShipment] = useState(null);
    const [editingStatus, setEditingStatus] = useState(false);
    const [newStatus, setNewStatus] = useState('');
    const [updatingStatus, setUpdatingStatus] = useState(false);

    useEffect(() => {
        fetchShipments();
    }, []);

    const fetchShipments = async () => {
        setLoading(true);
        try {
            const response = await shipmentApi.get('/shipments', {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`, // Adjust based on your auth
                    'Content-Type': 'application/json'
                }
            });


            const data = response.data;
            setShipments(data);
            setError(null);
        } catch (err) {
            setError('Failed to fetch shipments');
            console.error('Error fetching shipments:', err);
        } finally {
            setLoading(false);
        }
    };

    const fetchShipmentDetails = async (shipmentId) => {
        try {
            const response = await shipmentApi.get(`/api/shipments/${shipmentId}`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`,
                    'Content-Type': 'application/json'
                }
            });

            const data = response.data;
            setSelectedShipment(data);
        } catch (err) {
            setError('Failed to fetch shipment details');
            console.error('Error fetching shipment details:', err);
        }
    };

    const updateShipmentStatus = async (shipmentId, status) => {
        setUpdatingStatus(true);
        try {
            const response = await shipmentApi.patch(`/shipments/${shipmentId}/status`, {
                method: 'PATCH',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({status})
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const updatedShipment = await response.json();

            // Update shipments list
            setShipments(prev => prev.map(shipment =>
                shipment.id === shipmentId ? updatedShipment : shipment
            ));

            // Update selected shipment if it's the one being updated
            if (selectedShipment?.id === shipmentId) {
                setSelectedShipment(updatedShipment);
            }

            setEditingStatus(false);
            setNewStatus('');
        } catch (err) {
            setError('Failed to update shipment status');
            console.error('Error updating shipment status:', err);
        } finally {
            setUpdatingStatus(false);
        }
    };

    const getStatusColor = (status) => {
        const colors = {
            CREATED: 'bg-yellow-100 text-yellow-800 border-yellow-200',
            IN_DELIVERY: 'bg-indigo-100 text-indigo-800 border-indigo-200',
            DELIVERED: 'bg-green-100 text-green-800 border-green-200',
            REJECTED: 'bg-red-100 text-red-800 border-red-200'
        };
        return colors[status] || 'bg-gray-100 text-gray-800 border-gray-200';
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        const options = {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        };
        return new Date(dateString).toLocaleDateString(undefined, options);
    };

    const formatStatusForDisplay = (status) => {
        return status?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Unknown';
    };

    const statusOptions = [
        'CREATED', 'IN_DELIVERY', 'DELIVERED', 'REJECTED',
    ];

    const handleStatusEdit = (currentStatus) => {
        setEditingStatus(true);
        setNewStatus(currentStatus);
    };

    const handleStatusCancel = () => {
        setEditingStatus(false);
        setNewStatus('');
    };

    const handleStatusSave = () => {
        if (newStatus && selectedShipment) {
            updateShipmentStatus(selectedShipment.id, newStatus);
        }
    };

    if (loading && !shipments.length) {
        return (
            <div className="p-8 flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                <span className="ml-2">Loading shipments...</span>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-4">
                <div className="text-red-600 bg-red-50 border border-red-200 rounded p-4 mb-4">
                    {error}
                </div>
                <button
                    onClick={fetchShipments}
                    className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center"
                >
                    <span className="mr-2">🔄</span>
                    Retry
                </button>
            </div>
        );
    }

    if (shipments.length === 0) {
        return (
            <div className="p-8 text-center">
                <div className="w-12 h-12 bg-gray-200 rounded"/>
                <p className="text-gray-500">No shipments found</p>
                <button
                    onClick={fetchShipments}
                    className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center mx-auto"
                >
                    <span className="mr-2">🔄</span>
                    Refresh
                </button>
            </div>
        );
    }

    return (
        <div className="p-6 max-w-7xl mx-auto">
            <div className="mb-6 flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 flex items-center">
                        <span className="mr-2">🚚</span>
                        Shipment Management
                    </h1>
                    <p className="text-gray-600 mt-1">Manage and track all shipments ({shipments.length} total)</p>
                </div>
                <button
                    onClick={fetchShipments}
                    disabled={loading}
                    className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 flex items-center"
                >
                    <span className="mr-2">🔄</span> Refresh
                </button>
            </div>

            <div className="flex gap-6">
                {/* Shipments List */}
                <div className="w-2/5">
                    <div className="bg-white rounded-lg shadow-sm border">
                        <div className="p-4 border-b bg-gray-50">
                            <h2 className="font-semibold text-gray-900">All Shipments</h2>
                        </div>
                        <div className="max-h-96 overflow-y-auto">
                            {shipments.map(shipment => (
                                <div
                                    key={shipment.id}
                                    className={`p-4 border-b cursor-pointer hover:bg-gray-50 transition-colors ${
                                        selectedShipment?.id === shipment.id ? 'bg-blue-50 border-blue-200' : ''
                                    }`}
                                    onClick={() => fetchShipmentDetails(shipment.id)}
                                >
                                    <div className="flex justify-between items-start mb-2">
                                        <div>
                                            <p className="font-semibold text-gray-900">Shipment #{shipment.id}</p>
                                            {shipment.order && (
                                                <p className="text-sm text-gray-600">Order: #{shipment.order.id}</p>
                                            )}
                                        </div>
                                        <span
                                            className={`px-2 py-1 text-xs font-medium rounded-full border ${getStatusColor(shipment.status)}`}>
                                            {formatStatusForDisplay(shipment.status)}
                                        </span>
                                    </div>

                                    <div className="space-y-1 text-sm text-gray-600">
                                        <p className="flex items-center">
                                            <span className="mr-1">📅</span>
                                            Created: {formatDate(shipment.createdAt)}
                                        </p>
                                        {shipment.updatedAt !== shipment.createdAt && (
                                            <p className="flex items-center">
                                                <span className="mr-2">🔄</span>
                                                Updated: {formatDate(shipment.updatedAt)}
                                            </p>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Shipment Details */}
                <div className="w-3/5">
                    <div className="bg-white rounded-lg shadow-sm border">
                        {selectedShipment ? (
                            <div className="p-6">
                                <div className="flex justify-between items-start mb-6">
                                    <div>
                                        <h3 className="text-xl font-bold text-gray-900">
                                            Shipment #{selectedShipment.id}
                                        </h3>
                                        {selectedShipment.order && (
                                            <p className="text-gray-600">Order #{selectedShipment.order.id}</p>
                                        )}
                                    </div>
                                    <div className="flex items-center space-x-2">
                                        {editingStatus ? (
                                            <div className="flex items-center space-x-2">
                                                <select
                                                    value={newStatus}
                                                    onChange={(e) => setNewStatus(e.target.value)}
                                                    className="px-3 py-1 border rounded text-sm"
                                                    disabled={updatingStatus}
                                                >
                                                    {statusOptions.map(status => (
                                                        <option key={status} value={status}>
                                                            {formatStatusForDisplay(status)}
                                                        </option>
                                                    ))}
                                                </select>
                                                <button
                                                    onClick={handleStatusSave}
                                                    disabled={updatingStatus}
                                                    className="p-1 text-green-600 hover:bg-green-50 rounded disabled:opacity-50"
                                                >
                                                    <span>✔️</span>
                                                </button>
                                                <button
                                                    onClick={handleStatusCancel}
                                                    disabled={updatingStatus}
                                                    className="p-1 text-red-600 hover:bg-red-50 rounded disabled:opacity-50"
                                                >
                                                    <X className="w-4 h-4"/>
                                                </button>
                                            </div>
                                        ) : (
                                            <div className="flex items-center space-x-2">
                                                <span
                                                    className={`px-3 py-1 text-sm font-medium rounded-full border ${getStatusColor(selectedShipment.status)}`}>
                                                    {formatStatusForDisplay(selectedShipment.status)}
                                                </span>
                                                <button
                                                    onClick={() => handleStatusEdit(selectedShipment.status)}
                                                    className="p-1 text-blue-600 hover:bg-blue-50 rounded"
                                                    title="Edit Status"
                                                >
                                                    <span className="text-sm font-medium">✏️</span>
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                <div className="grid grid-cols-2 gap-6 mb-6">
                                    <div className="space-y-4">
                                        <div>
                                            <h4 className="font-semibold text-gray-900 mb-2">Shipment Information</h4>
                                            <div className="space-y-2 text-sm">
                                                <p><span
                                                    className="font-medium">Created:</span> {formatDate(selectedShipment.createdAt)}
                                                </p>
                                                <p><span
                                                    className="font-medium">Last Updated:</span> {formatDate(selectedShipment.updatedAt)}
                                                </p>
                                                <p><span
                                                    className="font-medium">Status:</span> {formatStatusForDisplay(selectedShipment.status)}
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    {selectedShipment.order && (
                                        <div>
                                            <h4 className="font-semibold text-gray-900 mb-2">Order Information</h4>
                                            <div className="space-y-2 text-sm">
                                                <p><span
                                                    className="font-medium">Order ID:</span> #{selectedShipment.order.id}
                                                </p>
                                                {selectedShipment.order.user && (
                                                    <>
                                                        <p><span
                                                            className="font-medium">Customer:</span> {selectedShipment.order.user.firstName} {selectedShipment.order.user.lastName}
                                                        </p>
                                                        <p><span
                                                            className="font-medium">Email:</span> {selectedShipment.order.user.email}
                                                        </p>
                                                    </>
                                                )}
                                                {selectedShipment.order.totalAmount && (
                                                    <p><span
                                                        className="font-medium">Total:</span> ${selectedShipment.order.totalAmount.toFixed(2)}
                                                    </p>
                                                )}
                                                {selectedShipment.order.shippingAddress && (
                                                    <p><span
                                                        className="font-medium">Shipping Address:</span> {selectedShipment.order.shippingAddress}
                                                    </p>
                                                )}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {selectedShipment.order?.items && selectedShipment.order.items.length > 0 && (
                                    <div>
                                        <h4 className="font-semibold text-gray-900 mb-3">Order Items</h4>
                                        <div className="overflow-x-auto">
                                            <table className="w-full text-sm">
                                                <thead>
                                                <tr className="bg-gray-50 border-b">
                                                    <th className="text-left p-3 font-medium">Product</th>
                                                    <th className="text-right p-3 font-medium">Price</th>
                                                    <th className="text-right p-3 font-medium">Quantity</th>
                                                    <th className="text-right p-3 font-medium">Subtotal</th>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                {selectedShipment.order.items.map((item, index) => (
                                                    <tr key={index} className="border-b">
                                                        <td className="p-3">{item.productName || item.product?.name || 'Unknown Product'}</td>
                                                        <td className="p-3 text-right">${(item.unitPrice || item.price || 0).toFixed(2)}</td>
                                                        <td className="p-3 text-right">{item.quantity || 0}</td>
                                                        <td className="p-3 text-right">
                                                            ${((item.unitPrice || item.price || 0) * (item.quantity || 0)).toFixed(2)}
                                                        </td>
                                                    </tr>
                                                ))}
                                                {selectedShipment.order.totalAmount && (
                                                    <tr className="font-semibold bg-gray-50">
                                                        <td className="p-3" colSpan="3">Total</td>
                                                        <td className="p-3 text-right">
                                                            ${selectedShipment.order.totalAmount.toFixed(2)}
                                                        </td>
                                                    </tr>
                                                )}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="flex flex-col items-center justify-center h-96 text-gray-500">
                                <div className="w-12 h-12 bg-gray-200 rounded"/>
                                <p className="text-lg">Select a shipment to view details</p>
                                <p className="text-sm">Click on any shipment from the list to see its information</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ShipmentAdmin;