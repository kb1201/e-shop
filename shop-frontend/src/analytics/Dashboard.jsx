import {useEffect, useState} from "react";
import {
    LineChart,
    AreaChart,
    Area,
    PieChart,
    Pie,
    Bar,
    BarChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer
} from "recharts";
import ChartCard from "./ChartCard";
import {analyticsApi} from "../api";

const Dashboard = () => {
    const [dailyRevenue, setDailyRevenue] = useState([]);
    const [paymentMethods, setPaymentMethods] = useState([]);
    const [orderStatus, setOrderStatus] = useState([]);
    const [topProducts, setTopProducts] = useState([]);
    const [hourlySales, setHourlySales] = useState([]);
    const [monthlyGrowth, setMonthlyGrowth] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchAllData = async () => {
            try {
                setLoading(true);

                const [
                    dailyRevenueRes,
                    paymentMethodsRes,
                    orderStatusRes,
                    topProductsRes,
                    hourlySalesRes,
                    monthlyGrowthRes
                ] = await Promise.all([
                    analyticsApi.get("/daily-revenue", {days: 30}),
                    analyticsApi.get("/revenue-by-payment", {days: 30}),
                    analyticsApi.get("/order-status", {days: 30}),
                    analyticsApi.get("/top-products", {days: 30, limit: 10}),
                    analyticsApi.get("/hourly-sales", {days: 7}),
                    analyticsApi.get("/monthly-growth", {days: 365}),
                ]);

                setDailyRevenue(dailyRevenueRes.data);
                setPaymentMethods(paymentMethodsRes.data);
                setOrderStatus(orderStatusRes.data);
                setTopProducts(topProductsRes.data);
                setHourlySales(hourlySalesRes.data);
                setMonthlyGrowth(monthlyGrowthRes.data);
            } catch (error) {
                console.error("Failed to fetch analytics data", error);
            } finally {
                setLoading(false);
            }
        };

        fetchAllData();
    }, []);


    const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

    return (
        <div className="min-h-screen bg-gray-50 p-6">
            <div className="max-w-7xl mx-auto">
                <h1 className="text-3xl font-bold text-gray-900 mb-8">Analytics Dashboard</h1>

                {/* Top Row - Revenue and Orders */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                    <ChartCard title="Daily Revenue & Orders" loading={loading}>
                        <ResponsiveContainer width="100%" height={300}>
                            <LineChart data={dailyRevenue}>
                                <CartesianGrid strokeDasharray="3 3"/>
                                <XAxis
                                    dataKey="date"
                                    tickFormatter={(value) => new Date(value).toLocaleDateString()}
                                />
                                <YAxis yAxisId="revenue" orientation="left"/>
                                <YAxis yAxisId="orders" orientation="right"/>
                                <Tooltip
                                    labelFormatter={(value) => new Date(value).toLocaleDateString()}
                                    formatter={(value, name) => [
                                        name === 'total_revenue' ? `$${value.toLocaleString()}` : value,
                                        name === 'total_revenue' ? 'Revenue' : 'Orders'
                                    ]}
                                />
                                <Line
                                    yAxisId="revenue"
                                    type="monotone"
                                    dataKey="total_revenue"
                                    stroke="#8884d8"
                                    strokeWidth={2}
                                />
                                <Line
                                    yAxisId="orders"
                                    type="monotone"
                                    dataKey="total_orders"
                                    stroke="#82ca9d"
                                    strokeWidth={2}
                                />
                            </LineChart>
                        </ResponsiveContainer>
                    </ChartCard>

                    <ChartCard title="Monthly Growth Trend" loading={loading}>
                        <ResponsiveContainer width="100%" height={300}>
                            <AreaChart data={monthlyGrowth}>
                                <CartesianGrid strokeDasharray="3 3"/>
                                <XAxis dataKey="month"/>
                                <YAxis/>
                                <Tooltip
                                    formatter={(value, name) => [
                                        name === 'revenue' ? `$${value.toLocaleString()}` : `${value}%`,
                                        name === 'revenue' ? 'Revenue' : 'Growth Rate'
                                    ]}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="revenue"
                                    stackId="1"
                                    stroke="#8884d8"
                                    fill="#8884d8"
                                    fillOpacity={0.6}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </ChartCard>
                </div>

                {/* Second Row - Payment Methods and Order Status */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                    <ChartCard title="Revenue by Payment Method" loading={loading}>
                        <ResponsiveContainer width="100%" height={300}>
                            <PieChart>
                                <Pie
                                    data={paymentMethods}
                                    cx="50%"
                                    cy="50%"
                                    labelLine={false}
                                    label={({payment_method, percentage}) => `${payment_method} (${percentage}%)`}
                                    outerRadius={80}
                                    fill="#8884d8"
                                    dataKey="total_revenue"
                                >
                                    {paymentMethods.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]}/>
                                    ))}
                                </Pie>
                                <Tooltip formatter={(value) => [`$${value.toLocaleString()}`, 'Revenue']}/>
                            </PieChart>
                        </ResponsiveContainer>
                    </ChartCard>

                    <ChartCard title="Order Status Distribution" loading={loading}>
                        <ResponsiveContainer width="100%" height={300}>
                            <BarChart data={orderStatus}>
                                <CartesianGrid strokeDasharray="3 3"/>
                                <XAxis dataKey="status"/>
                                <YAxis/>
                                <Tooltip formatter={(value) => [value.toLocaleString(), 'Orders']}/>
                                <Bar dataKey="count" fill="#82ca9d"/>
                            </BarChart>
                        </ResponsiveContainer>
                    </ChartCard>
                </div>

                {/* Third Row - Products and Hourly Sales */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                    <ChartCard title="Top Selling Products" loading={loading}>
                        <ResponsiveContainer width="100%" height={300}>
                            <BarChart data={topProducts} layout="horizontal">
                                <CartesianGrid strokeDasharray="3 3"/>
                                <XAxis type="number"/>
                                <YAxis
                                    dataKey="product_name"
                                    type="category"
                                    width={120}
                                    tick={{fontSize: 12}}
                                />
                                <Tooltip
                                    formatter={(value, name) => [
                                        name === 'revenue' ? `$${value.toLocaleString()}` : value,
                                        name === 'revenue' ? 'Revenue' : 'Units Sold'
                                    ]}
                                />
                                <Bar dataKey="total_sold" fill="#8884d8"/>
                            </BarChart>
                        </ResponsiveContainer>
                    </ChartCard>

                    <ChartCard title="Hourly Sales Pattern" loading={loading}>
                        <ResponsiveContainer width="100%" height={300}>
                            <AreaChart data={hourlySales}>
                                <CartesianGrid strokeDasharray="3 3"/>
                                <XAxis
                                    dataKey="hour"
                                    tickFormatter={(value) => `${value}:00`}
                                />
                                <YAxis/>
                                <Tooltip
                                    labelFormatter={(value) => `${value}:00`}
                                    formatter={(value, name) => [
                                        name === 'sales' ? `$${value.toLocaleString()}` : value,
                                        name === 'sales' ? 'Sales' : 'Orders'
                                    ]}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="sales"
                                    stroke="#FF8042"
                                    fill="#FF8042"
                                    fillOpacity={0.6}
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </ChartCard>
                </div>

                {/* Summary Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
                        <h3 className="text-sm font-medium text-gray-500">Total Revenue</h3>
                        <p className="text-2xl font-bold text-gray-900">
                            ${dailyRevenue.reduce((sum, day) => sum + day.total_revenue, 0).toLocaleString()}
                        </p>
                    </div>
                    <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
                        <h3 className="text-sm font-medium text-gray-500">Total Orders</h3>
                        <p className="text-2xl font-bold text-gray-900">
                            {dailyRevenue.reduce((sum, day) => sum + day.total_orders, 0).toLocaleString()}
                        </p>
                    </div>
                    <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
                        <h3 className="text-sm font-medium text-gray-500">Avg Order Value</h3>
                        <p className="text-2xl font-bold text-gray-900">
                            ${dailyRevenue.length > 0 ?
                            Math.round(
                                dailyRevenue.reduce((sum, day) => sum + day.total_revenue, 0) /
                                dailyRevenue.reduce((sum, day) => sum + day.total_orders, 0)
                            ) : 0
                        }
                        </p>
                    </div>
                    <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
                        <h3 className="text-sm font-medium text-gray-500">Active Products</h3>
                        <p className="text-2xl font-bold text-gray-900">{topProducts.length}</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;