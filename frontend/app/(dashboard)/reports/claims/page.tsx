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

export default function ClaimsReportPage() {
  const claimsByStatus = [
    { name: 'Submitted', value: 45, color: '#6366f1' },
    { name: 'Under Review', value: 32, color: '#f59e0b' },
    { name: 'Approved', value: 68, color: '#10b981' },
    { name: 'Rejected', value: 12, color: '#ef4444' },
    { name: 'Settled', value: 89, color: '#3b82f6' },
  ];

  const claimsByType = [
    { name: 'Collision', value: 85 },
    { name: 'Theft', value: 23 },
    { name: 'Vandalism', value: 18 },
    { name: 'Natural Disaster', value: 12 },
    { name: 'Fire', value: 8 },
  ];

  const monthlyClaims = [
    { month: 'Jan', submitted: 45, settled: 38, rejected: 5 },
    { month: 'Feb', submitted: 52, settled: 45, rejected: 7 },
    { month: 'Mar', submitted: 48, settled: 42, rejected: 4 },
    { month: 'Apr', submitted: 61, settled: 55, rejected: 6 },
    { month: 'May', submitted: 58, settled: 52, rejected: 5 },
    { month: 'Jun', submitted: 65, settled: 58, rejected: 7 },
  ];

  const processingTime = [
    { stage: 'Submission', avgDays: 0 },
    { stage: 'Review', avgDays: 3 },
    { stage: 'Assessment', avgDays: 7 },
    { stage: 'Approval', avgDays: 10 },
    { stage: 'Settlement', avgDays: 14 },
  ];

  return (
    <div className="p-6">
      <PageHeader title="Claims Report" />

      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Total Claims</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">246</div>
              <div className="text-sm text-gray-500">This month</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Pending</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">77</div>
              <div className="text-sm text-gray-500">Awaiting action</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Settled</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">89</div>
              <div className="text-sm text-gray-500">This month</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Avg. Processing</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">14 days</div>
              <div className="text-sm text-gray-500">From submission</div>
            </CardContent>
          </Card>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Claims by Status</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={claimsByStatus}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {claimsByStatus.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Claims by Type</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={claimsByType} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" />
                  <YAxis dataKey="name" type="category" width={100} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#3b82f6" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Monthly Claims Trend</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={monthlyClaims}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="submitted" fill="#6366f1" name="Submitted" />
                  <Bar dataKey="settled" fill="#10b981" name="Settled" />
                  <Bar dataKey="rejected" fill="#ef4444" name="Rejected" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Average Processing Time by Stage</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={processingTime}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="stage" />
                  <YAxis label={{ value: 'Days', angle: -90, position: 'insideLeft' }} />
                  <Tooltip />
                  <Line type="monotone" dataKey="avgDays" stroke="#3b82f6" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
