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
    const [page, setPage] = useState(0); // page is zero-based
    const [totalPages, setTotalPages] = useState(1);
    const pageSize = 10;
    const [totalElements, setTotalElements] = useState(0);

    useEffect(() => {
        fetchShipments(page);
    }, [page]);

    const isStatusFinal = (status) => {
        return status === 'DELIVERED' || status === 'REJECTED';
    };

    const getAvailableStatusOptions = (currentStatus) => {
        const allStatuses = ['CREATED', 'IN_DELIVERY', 'DELIVERED', 'REJECTED'];

        // If current status is final, no changes allowed
        if (isStatusFinal(currentStatus)) {
            return [currentStatus]; // Only show current status
        }

        if (currentStatus === 'IN_DELIVERY')
            return ['IN_DELIVERY', 'DELIVERED', 'REJECTED'];

        // For non-final statuses, allow all transitions
        // You can add more specific business rules here if needed
        return allStatuses;
    };

    const fetchShipments = async (pageNumber = 0) => {
        setLoading(true);
        try {
            const response = await shipmentApi.get('/shipments', {
                params: {
                    page: pageNumber,
                    size: pageSize
                }
            });

            const data = response.data;
            setShipments(data.content);
            setPage(data.number);
            setTotalPages(data.totalPages);
            setError(null);
            setTotalElements(data.totalElements);
        } catch (err) {
            setError('Failed to fetch shipments');
            console.error('Error fetching shipments:', err);
        } finally {
            setLoading(false);
        }
    };

    const fetchShipmentDetails = async (shipmentId) => {
        try {
            const response = await shipmentApi.get(`/shipments/${shipmentId}`);

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
            const response = await shipmentApi.patch(`/shipments/${shipmentId}/status`, {status});

            const updatedShipment = response.data;

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
            setError(null); // Clear any previous errors

            // Show success message (optional)
            console.log('Shipment status updated successfully');

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

    const getOrderStatusColor = (status) => {
        const colors = {
            PROCESSING: 'bg-blue-100 text-blue-800 border-blue-200',
            SHIPPED: 'bg-purple-100 text-purple-800 border-purple-200',
            DELIVERED: 'bg-green-100 text-green-800 border-green-200',
            CANCELLED: 'bg-red-100 text-red-800 border-red-200'
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

    // Available shipment statuses that admin can set
    const shipmentStatusOptions = [
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
        if (newStatus && selectedShipment && newStatus !== selectedShipment.status) {
            updateShipmentStatus(selectedShipment.id, newStatus);
        } else {
            // No change, just cancel editing
            handleStatusCancel();
        }
    };

    // Fixed: Use proper numeric values for page navigation
    const handlePreviousPage = () => {
        const newPage = Math.max(page - 1, 0);
        setPage(newPage);
    };

    const handleNextPage = () => {
        const newPage = Math.min(page + 1, totalPages - 1);
        setPage(newPage);
    };

    const handleRefresh = () => {
        fetchShipments(page);
    };

    if (loading && !totalElements) {
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
                    onClick={handleRefresh}
                    className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 flex items-center"
                >
                    <span className="mr-2">🔄</span>
                    Retry
                </button>
            </div>
        );
    }

    if (totalElements === 0) {
        return (
            <div className="p-8 text-center">
                <div className="w-12 h-12 bg-gray-200 rounded"/>
                <p className="text-gray-500">No shipments found</p>
                <button
                    onClick={handleRefresh}
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
                    <p className="text-gray-600 mt-1">Manage and track all shipments ({totalElements} total)</p>
                </div>
                <button
                    onClick={handleRefresh}
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
                                        <div className="flex flex-col items-end space-y-1">
                                            <span
                                                className={`px-2 py-1 text-xs font-medium rounded-full border ${getStatusColor(shipment.status)}`}>
                                                {formatStatusForDisplay(shipment.status)}
                                            </span>
                                            {shipment.order?.status && (
                                                <span
                                                    className={`px-2 py-1 text-xs font-medium rounded-full border ${getOrderStatusColor(shipment.order.status)}`}>
                                                    Order: {formatStatusForDisplay(shipment.order.status)}
                                                </span>
                                            )}
                                        </div>
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

                        {/* Fixed: Moved pagination outside of the shipment list and fixed the handlers */}
                        <div className="px-4 py-4 border-t bg-gray-50">
                            <div className="flex justify-center items-center space-x-4">
                                <button
                                    onClick={handlePreviousPage}
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
                                    onClick={handleNextPage}
                                    disabled={page >= totalPages - 1}
                                    className="px-4 py-2 rounded-xl border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm transition"
                                >
                                    Next →
                                </button>
                            </div>
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
                                            <p className="text-gray-600">Order #{selectedShipment.order.orderId}</p>
                                        )}
                                    </div>
                                    <div className="flex flex-col items-end space-y-2">
                                        {/* Shipment Status */}
                                        {editingStatus ? (
                                            <div className="flex items-center space-x-2">
                                                <select
                                                    value={newStatus}
                                                    onChange={(e) => setNewStatus(e.target.value)}
                                                    className="px-3 py-1 border rounded text-sm"
                                                    disabled={updatingStatus}
                                                >
                                                    {getAvailableStatusOptions(selectedShipment.status).map(status => (
                                                        <option key={status} value={status}>
                                                            {formatStatusForDisplay(status)}
                                                        </option>
                                                    ))}
                                                </select>
                                                <button
                                                    onClick={handleStatusSave}
                                                    disabled={updatingStatus || isStatusFinal(selectedShipment.status)}
                                                    className="p-1 text-green-600 hover:bg-green-50 rounded disabled:opacity-50"
                                                    title="Save Status"
                                                >
                                                    {updatingStatus ? '⏳' : '✅'}
                                                </button>
                                                <button
                                                    onClick={handleStatusCancel}
                                                    disabled={updatingStatus}
                                                    className="p-1 text-red-600 hover:bg-red-50 rounded disabled:opacity-50"
                                                    title="Cancel"
                                                >
                                                    ❌
                                                </button>
                                            </div>
                                        ) : (
                                            <div className="flex items-center space-x-2">
                                                <span
                                                    className={`px-3 py-1 text-sm font-medium rounded-full border ${getStatusColor(selectedShipment.status)}`}>
                                                    Shipment: {formatStatusForDisplay(selectedShipment.status)}
                                                </span>
                                                {!isStatusFinal(selectedShipment.status) ? (
                                                    <button
                                                        onClick={() => handleStatusEdit(selectedShipment.status)}
                                                        className="p-1 text-blue-600 hover:bg-blue-50 rounded"
                                                        title="Edit Shipment Status"
                                                    >
                                                        <span className="text-sm font-medium">✏️</span>
                                                    </button>
                                                ) : (
                                                    <span
                                                        className="p-1 text-gray-400"
                                                        title="Final status - cannot be changed"
                                                    >
                                                        🔒
                                                    </span>
                                                )}
                                            </div>
                                        )}

                                        {/* Order Status (read-only) */}
                                        {selectedShipment.order?.status && (
                                            <span
                                                className={`px-3 py-1 text-sm font-medium rounded-full border ${getOrderStatusColor(selectedShipment.order.status)}`}>
                                                Order: {formatStatusForDisplay(selectedShipment.order.status)}
                                            </span>
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
                                                    className="font-medium">Shipment Status:</span> {formatStatusForDisplay(selectedShipment.status)}
                                                </p>
                                                {selectedShipment.order?.status && (
                                                    <p><span
                                                        className="font-medium">Order Status:</span> {formatStatusForDisplay(selectedShipment.order.status)}
                                                    </p>
                                                )}
                                            </div>
                                        </div>
                                    </div>

                                    {selectedShipment.order && (
                                        <div>
                                            <h4 className="font-semibold text-gray-900 mb-2">Order Information</h4>
                                            <div className="space-y-2 text-sm">
                                                <p><span
                                                    className="font-medium">Order ID:</span> #{selectedShipment.order.orderId}
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
                                                {selectedShipment.order.billingAddress && (
                                                    <p><span
                                                        className="font-medium">Billing Address:</span> {selectedShipment.order.billingAddress}
                                                    </p>
                                                )}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Shipping and Billing Addresses */}
                                {(selectedShipment.order?.shippingAddress || selectedShipment.order?.billingAddress) && (
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                                        {selectedShipment.order.shippingAddress && (
                                            <div>
                                                <h4 className="font-semibold text-gray-900 mb-2 flex items-center">
                                                    <span className="mr-1">🚚</span>
                                                    Shipping Address
                                                </h4>
                                                <div className="bg-gray-50 p-3 rounded text-sm">
                                                    <p>{selectedShipment.order.shippingAddress}</p>
                                                </div>
                                            </div>
                                        )}

                                        {selectedShipment.order.billingAddress && (
                                            <div>
                                                <h4 className="font-semibold text-gray-900 mb-2 flex items-center">
                                                    <span className="mr-1">💳</span>
                                                    Billing Address
                                                </h4>
                                                <div className="bg-gray-50 p-3 rounded text-sm">
                                                    <p>{selectedShipment.order.billingAddress}</p>
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Status Mapping Information */}
                                <div className="mt-6 p-4 bg-gray-50 rounded-lg">
                                    <h4 className="font-semibold text-gray-900 mb-2">Status Mapping</h4>
                                    <p className="text-sm text-gray-600 mb-2">When you update the shipment status, the
                                        order status will automatically be updated:</p>
                                    <div className="grid grid-cols-2 gap-4 text-sm">
                                        <div>
                                            <span className="font-medium">CREATED</span> → Order: PROCESSING
                                        </div>
                                        <div>
                                            <span className="font-medium">IN_DELIVERY</span> → Order: SHIPPED
                                        </div>
                                        <div>
                                            <span className="font-medium">DELIVERED</span> → Order: DELIVERED
                                        </div>
                                        <div>
                                            <span className="font-medium">REJECTED</span> → Order: CANCELLED
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="flex flex-col items-center justify-center h-96 text-gray-500">
                                <div className="w-12 h-12 bg-gray-200 rounded mb-4"/>
                                <p className="text-lg">Select a shipment to view details</p>
                                <p className="text-sm">Click on any shipment from the list to see its information and
                                    update its status</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ShipmentAdmin;