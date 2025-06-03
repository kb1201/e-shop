import React, { useEffect, useState } from 'react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import { analyticsApi } from '../api';

export default function ShipmentStatusDurationChart() {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get('/shipment/status-duration');
                setData(response.data);
                setError('');
            } catch {
                setError('Failed to load shipment status durations.');
            } finally {
                setLoading(false);
            }
        }
        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading shipment status durations...</div>;
    if (error) return <div className="text-red-600">{error}</div>;

    return (
        <div className="max-w-6xl mx-auto p-6 bg-white rounded-xl shadow">
            <h2 className="text-2xl font-semibold text-gray-800 mb-6">Shipment Status Duration Analytics (Last 30 Days)</h2>
            <ResponsiveContainer width="100%" height={400}>
                <BarChart data={data} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="status" />
                    <YAxis label={{ value: 'Hours', angle: -90, position: 'insideLeft' }} />
                    <Tooltip />
                    <Legend verticalAlign="top" />
                    <Bar dataKey="avgHoursInStatus" fill="#3b82f6" name="Avg Hours" />
                    <Bar dataKey="medianHoursInStatus" fill="#f59e0b" name="Median Hours" />
                    <Bar dataKey="p95HoursInStatus" fill="#ef4444" name="95th Percentile Hours" />
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}
