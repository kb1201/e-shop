import React, { useEffect, useState } from 'react';
import { analyticsApi } from '../api';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';

export default function CartAnalyticsChart() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get('/cart/summary');
                setData(response.data);
                setError('');
            } catch {
                setError('Failed to load cart analytics.');
            } finally {
                setLoading(false);
            }
        }
        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading cart analytics...</div>;
    if (error) return <div className="text-red-600">{error}</div>;

    // Prepare data for abandonment bar chart
    const abandonmentData = [
        { name: 'Abandoned > 24h', value: data.cartsAbandoned24h },
        { name: 'Abandoned > 72h', value: data.cartsAbandoned72h },
        { name: 'Abandoned > 1 week', value: data.cartsAbandoned1week },
    ];

    // Conversion data for Pie or Bar (optional, here as bars)
    const conversionData = [
        { name: 'Users w/ No Recent Order', value: data.usersWithCartNoRecentOrder },
        { name: 'Users w/ Recent Order', value: data.totalActiveCarts - data.usersWithCartNoRecentOrder },
    ];

    return (
        <div className="max-w-7xl mx-auto p-6 bg-white rounded-xl shadow space-y-10">
            <h2 className="text-2xl font-semibold text-gray-800">Cart Analytics (Last 7 Days)</h2>

            <div className="grid md:grid-cols-2 gap-10">
                {/* Cart Metrics Summary */}
                <div className="p-4 bg-gray-50 rounded-lg shadow-inner">
                    <h3 className="text-lg font-medium text-gray-700 mb-4">Cart Summary</h3>
                    <ul className="space-y-2 text-gray-700">
                        <li><strong>Total Active Carts:</strong> {data.totalActiveCarts}</li>
                        <li><strong>Total Items in Carts:</strong> {data.totalItemsInCarts}</li>
                        <li><strong>Total Cart Value:</strong> ${data.totalCartValue.toFixed(2)}</li>
                        <li><strong>Average Cart Value:</strong> ${data.avgCartValue}</li>
                        <li><strong>Average Items per Cart:</strong> {data.avgItemsPerCart}</li>
                    </ul>
                </div>

                {/* Abandonment Bar Chart */}
                <div>
                    <h3 className="text-lg font-medium text-gray-700 mb-2">Cart Abandonment</h3>
                    <ResponsiveContainer width="100%" height={300}>
                        <BarChart data={abandonmentData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="name" />
                            <YAxis />
                            <Tooltip />
                            <Legend />
                            <Bar dataKey="value" fill="#f97316" name="Count" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>

                {/* Conversion Opportunity Bar Chart */}
                <div>
                    <h3 className="text-lg font-medium text-gray-700 mb-2">Conversion Opportunity</h3>
                    <ResponsiveContainer width="100%" height={300}>
                        <BarChart data={conversionData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="name" />
                            <YAxis />
                            <Tooltip />
                            <Legend />
                            <Bar dataKey="value" fill="#10b981" name="Users" />
                        </BarChart>
                    </ResponsiveContainer>
                    <p className="mt-3 text-gray-600">
                        No recent order rate: <strong>{data.noRecentOrderRatePct}%</strong>
                    </p>
                </div>
            </div>
        </div>
    );
}
