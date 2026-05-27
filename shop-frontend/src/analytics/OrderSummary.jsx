import React, {useEffect, useState} from 'react';
import {PieChart,Legend , Pie, Cell, Tooltip, ResponsiveContainer} from 'recharts';
import {analyticsApi} from "../api";

// Colors for pie segments
const STATUS_COLORS = ['#3b82f6', '#f87171', '#facc15'];
const REVENUE_COLORS = ['#10b981', '#3b82f6', '#ef4444'];

// Component: Order Status Pie Chart
function OrderStatusPie({
                            pending,
                            paid,
                            processing,
                            shipped,
                            delivered,
                            cancelled,
                            refunded,
                        }) {
    const data = [
        { name: "Pending", value: pending, color: "#6366f1" },
        { name: "Paid", value: paid, color: "#3b82f6" },
        { name: "Processing", value: processing, color: "#06b6d4" },
        { name: "Shipped", value: shipped, color: "#10b981" },
        { name: "Delivered", value: delivered, color: "#f59e0b" },
        { name: "Cancelled", value: cancelled, color: "#ef4444" },
        { name: "Refunded", value: refunded, color: "#a855f7" },
    ].filter(item => item.value > 0);

    return (
        <div>
            <h3 className="text-lg font-semibold text-gray-800 mb-2">Order Status Breakdown</h3>
            <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                    <Pie
                        data={data}
                        cx="50%"
                        cy="50%"
                        outerRadius={100}
                        dataKey="value"
                        nameKey="name"
                    >
                        {data.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                    </Pie>
                    <Tooltip />
                    <Legend verticalAlign="bottom" height={36} />
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
                const response = await analyticsApi.get("/analytics/order/summary");
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
        <div className="p-6 max-w-5xl mx-auto space-y-8 bg-white rounded-xl shadow">
            <h2 className="text-2xl font-bold text-gray-800">Order Analytics (Last 30 Days)</h2>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-gray-700 text-base">
                <div className="p-4 rounded-lg bg-blue-50">
                    <div>Total Orders</div>
                    <div className="text-xl font-semibold text-blue-600">{data.totalOrders}</div>
                </div>
                <div className="p-4 rounded-lg bg-amber-50">
                    <div>Avg Order Value</div>
                    <div className="text-xl font-semibold text-amber-600">${data.avgOrderValue.toFixed(2)}</div>
                </div>
                <div className="p-4 rounded-lg bg-blue-50">
                    <div>Delivery Rate</div>
                    <div className="text-xl font-semibold text-blue-600">{data.deliveryRatePct}%</div>
                </div>
                <div className="p-4 rounded-lg bg-red-50">
                    <div>Cancellation Rate</div>
                    <div className="text-xl font-semibold text-red-600">{data.cancellationRatePct}%</div>
                </div>
            </div>

            <div className="flex flex-col md:flex-row gap-6">
                <div className="flex-1 bg-gray-50 p-4 rounded-lg">
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

                <div className="flex-1 bg-gray-50 p-4 rounded-lg">
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
