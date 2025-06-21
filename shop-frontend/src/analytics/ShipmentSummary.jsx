import React, {useEffect, useState} from 'react';
import {
    PieChart, Pie, Cell, Tooltip, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Legend, CartesianGrid
} from 'recharts';
import {analyticsApi} from '../api';

const COLORS = ['#10b981', '#3b82f6', '#f97316', '#f43f5e', '#a855f7'];

export default function ShipmentAnalyticsChart() {
    const [data, setData] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get('/shipment/summary');
                setData(response.data);
                setError('');
            } catch {
                setError('Failed to load shipment analytics.');
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading shipment analytics...</div>;
    if (error) return <div className="text-red-600">{error}</div>;

    const statusData = [
        {name: 'Delivered', value: data.deliveredShipments},
        {name: 'In Transit', value: data.inTransitShipments},
        {name: 'Rejected', value: data.rejectedShipments},
        {name: 'Pending', value: data.pendingShipments}
    ];

    const metricsData = [
        {
            name: 'Delivery Metrics',
            'Success Rate (%)': data.deliverySuccessRatePct,
            'Rejection Rate (%)': data.rejectionRatePct,
            'Avg Delivery Time (h)': data.avgDeliveryTimeHours,
            'Median Delivery Time (h)': data.medianDeliveryTimeHours
        }
    ];

    return (
        <div className="p-6 bg-white rounded-xl shadow max-w-6xl mx-auto space-y-12">
            <h2 className="text-2xl font-semibold text-gray-800 mb-4">Shipment Analytics (Last 30 Days)</h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {/* Pie Chart */}
                <div>
                    <h3 className="text-lg font-medium text-gray-700 mb-2">Status Distribution</h3>
                    <ResponsiveContainer width="100%" height={300}>
                        <PieChart>
                            <Pie
                                data={statusData}
                                dataKey="value"
                                nameKey="name"
                                cx="50%"
                                cy="50%"
                                outerRadius={100}
                                label
                            >
                                {statusData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]}/>
                                ))}
                            </Pie>
                            <Legend verticalAlign="bottom"/>
                            <Tooltip/>
                        </PieChart>
                    </ResponsiveContainer>
                </div>

                {/* Bar Chart */}
                <div>
                    <h3 className="text-lg font-medium text-gray-700 mb-2">Delivery Timings & Rates</h3>
                    <ResponsiveContainer width="100%" height={300}>
                        <BarChart data={metricsData}>
                            <CartesianGrid strokeDasharray="3 3"/>
                            <XAxis dataKey="name"/>
                            <YAxis/>
                            <Tooltip/>
                            <Legend/>
                            <Bar dataKey="Success Rate (%)" fill="#10b981"/>
                            <Bar dataKey="Rejection Rate (%)" fill="#f43f5e"/>
                            <Bar dataKey=" Avg Delivery Time (h)" fill="#3b82f6"/>
                            <Bar dataKey="Median Delivery Time (h)" fill="#a855f7"/>
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>
        </div>
    );
}
