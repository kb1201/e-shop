import React, {useEffect, useState} from 'react';
import {PieChart, Pie, Cell, Tooltip, ResponsiveContainer} from 'recharts';
import {analyticsApi} from "../api";

// Colors for pie segments
const STATUS_COLORS = ['#3b82f6', '#f87171', '#facc15'];
const REVENUE_COLORS = ['#10b981', '#3b82f6', '#ef4444'];

// Component: Order Status Pie Chart
function OrderStatusPie({pending, paid, processing, shipped, delivered, cancelled, refunded}) {
    const data = [
        {name: 'Pending', value: pending},
        {name: 'Paid', value: paid},
        {name: 'Processing', value: processing},
        {name: 'Shipped', value: shipped},
        {name: 'Delivered', value: delivered},
        {name: 'Cancelled', value: cancelled},
        {name: 'Refunded', value: refunded},
    ];

    return (
        <div className="bg-white shadow rounded-xl p-4">
            <h3 className="text-lg font-medium mb-4">Order Status Breakdown</h3>
            <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                    <Pie
                        data={data}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        outerRadius={100}
                        label
                    >
                        {data.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={STATUS_COLORS[index % STATUS_COLORS.length]}/>
                        ))}
                    </Pie>
                    <Tooltip/>
                </PieChart>
            </ResponsiveContainer>
        </div>
    );
}

// Component: Revenue Breakdown Pie Chart
function RevenuePie({total, delivered, lost}) {
    const data = [
        {name: 'Total Revenue', value: total},
        {name: 'Delivered Revenue', value: delivered},
        {name: 'Lost Revenue', value: lost},
    ];

    return (
        <div className="bg-white shadow rounded-xl p-4">
            <h3 className="text-lg font-medium mb-4">Revenue Breakdown</h3>
            <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                    <Pie
                        data={data}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        outerRadius={100}
                        label
                    >
                        {data.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={REVENUE_COLORS[index % REVENUE_COLORS.length]}/>
                        ))}
                    </Pie>
                    <Tooltip/>
                </PieChart>
            </ResponsiveContainer>
        </div>
    );
}

// Main Dashboard Component
export default function OrderAnalyticsDashboard() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function fetchData() {
            try {
                const response = await analyticsApi.get("/order/summary");
                setData(response.data);
                setError(null);
            } catch (err) {
                setError("Failed to load order trends.");
            } finally {
                setLoading(false);
            }
        }

        fetchData();
    }, []);

    if (loading) return <div className="text-center py-10 text-gray-500">Loading order trends...</div>;
    if (error) return <div className="text-red-600">{error}</div>;

    return (
        <div className="p-6 max-w-4xl mx-auto space-y-6">
            <h2 className="text-2xl font-semibold text-gray-800">Order Analytics (Last 30 Days)</h2>

            <div className="grid grid-cols-2 gap-4 text-sm text-gray-700">
                <div>Total Orders: <strong>{data.totalOrders}</strong></div>
                <div>Avg Order Value: <strong>${data.avgOrderValue.toFixed(2)}</strong></div>
                <div>Delivery Rate: <strong>{data.deliveryRatePct}%</strong></div>
                <div>Cancellation Rate: <strong>{data.cancellationRatePct}%</strong></div>
            </div>

            <div className="flex flex-col md:flex-row gap-6">
                <div className="flex-1">
                    <OrderStatusPie
                        pending={data.pendingOrders}
                        paid={data.paidOrders}
                        processing={data.processingOrders}
                        shipped={data.shippedOrders}
                        delivered={data.deliveredOrders}
                        cancelled={data.cancelledOrders}
                        refunded={data.refundedOrders}
                    />
                </div>

                <div className="flex-1">
                    <RevenuePie
                        total={data.totalOrderValue}
                        delivered={data.deliveredRevenue}
                        lost={data.lostRevenue}
                    />
                </div>
            </div>
        </div>
    );
}
