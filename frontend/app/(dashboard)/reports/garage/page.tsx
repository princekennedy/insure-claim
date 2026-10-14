'use client';

import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from '@/lib/components/ui';
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

export default function GaragePerformanceReportPage() {
  const garageData = [
    { name: 'Auto Fix Ltd', repairs: 45, avgTime: 2.5, rating: 4.8 },
    { name: 'Quick Garage', repairs: 38, avgTime: 3.2, rating: 4.5 },
    { name: 'Elite Motors', repairs: 52, avgTime: 1.8, rating: 4.9 },
    { name: 'City Auto', repairs: 29, avgTime: 4.1, rating: 4.2 },
    { name: 'Premium Service', repairs: 41, avgTime: 2.9, rating: 4.6 },
  ];

  const monthlyData = [
    { month: 'Jan', repairs: 120, avgTime: 3.2 },
    { month: 'Feb', repairs: 145, avgTime: 2.9 },
    { month: 'Mar', repairs: 132, avgTime: 3.1 },
    { month: 'Apr', repairs: 158, avgTime: 2.7 },
    { month: 'May', repairs: 175, avgTime: 2.5 },
    { month: 'Jun', repairs: 168, avgTime: 2.6 },
  ];

  const statusData = [
    { name: 'Completed', value: 75, color: '#10b981' },
    { name: 'In Progress', value: 15, color: '#f59e0b' },
    { name: 'Pending', value: 10, color: '#6366f1' },
  ];

  return (
    <div className="p-6">
      <PageHeader title="Garage Performance Report" />

      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Total Repairs</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">205</div>
              <div className="text-sm text-gray-500">This month</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Average Repair Time</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">2.8 days</div>
              <div className="text-sm text-gray-500">Across all garages</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Average Rating</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">4.6/5</div>
              <div className="text-sm text-gray-500">Customer satisfaction</div>
            </CardContent>
          </Card>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Repairs by Garage</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={garageData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" angle={-45} textAnchor="end" height={100} />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="repairs" fill="#3b82f6" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Repair Status Distribution</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={statusData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {statusData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Monthly Repair Trends</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={monthlyData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis yAxisId="left" />
                <YAxis yAxisId="right" orientation="right" />
                <Tooltip />
                <Legend />
                <Bar yAxisId="left" dataKey="repairs" fill="#3b82f6" />
                <Line yAxisId="right" type="monotone" dataKey="avgTime" stroke="#f59e0b" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
