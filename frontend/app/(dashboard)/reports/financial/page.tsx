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
import { ReportExportActions } from '@/lib/components/reports/report-export-actions';

export default function FinancialSummaryReportPage() {
  const revenueData = [
    { month: 'Jan', premiums: 218750000, claims: 78750000, profit: 140000000 },
    { month: 'Feb', premiums: 231000000, claims: 91000000, profit: 140000000 },
    { month: 'Mar', premiums: 224000000, claims: 84000000, profit: 140000000 },
    { month: 'Apr', premiums: 253750000, claims: 101500000, profit: 152250000 },
    { month: 'May', premiums: 266000000, claims: 108500000, profit: 157500000 },
    { month: 'Jun', premiums: 276500000, claims: 113750000, profit: 162750000 },
  ];

  const expenseBreakdown = [
    { name: 'Claim Payouts', value: 577500000, color: '#ef4444' },
    { name: 'Operations', value: 148750000, color: '#f59e0b' },
    { name: 'Commissions', value: 78750000, color: '#6366f1' },
    { name: 'Marketing', value: 56000000, color: '#3b82f6' },
    { name: 'Other', value: 31500000, color: '#10b981' },
  ];

  const policyRevenue = [
    { type: 'Comprehensive', amount: 498750000, policies: 1250 },
    { type: 'Third Party', amount: 253750000, policies: 2100 },
    { type: 'Third Party Fire', amount: 171500000, policies: 680 },
  ];

  const claimRatio = [
    { month: 'Jan', ratio: 36 },
    { month: 'Feb', ratio: 39 },
    { month: 'Mar', ratio: 38 },
    { month: 'Apr', ratio: 40 },
    { month: 'May', ratio: 41 },
    { month: 'Jun', ratio: 41 },
  ];
  const reportTitle = 'Financial Summary Report';

  return (
    <div className="p-6">
      <PageHeader
        title={reportTitle}
        actions={
          <ReportExportActions
            title={reportTitle}
            sections={[
              {
                title: 'Summary',
                rows: [
                  { Metric: 'Total Revenue', Value: 'MK 1.47B', Notes: '+12.5% vs last month' },
                  { Metric: 'Total Claims Paid', Value: 'MK 577.5M', Notes: 'This quarter' },
                  { Metric: 'Net Profit', Value: 'MK 577.5M', Notes: '+8.2% vs last quarter' },
                  { Metric: 'Claim Ratio', Value: '39%', Notes: 'Industry avg: 42%' },
                ],
              },
              { title: 'Revenue vs Claims Trend', rows: revenueData },
              { title: 'Expense Breakdown', rows: expenseBreakdown.map(({ name, value }) => ({ name, value })) },
              { title: 'Revenue by Policy Type', rows: policyRevenue },
              { title: 'Claim Ratio Trend', rows: claimRatio },
            ]}
          />
        }
      />

      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Total Revenue</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">MK 1.47B</div>
              <div className="text-sm text-green-500">+12.5% vs last month</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Total Claims Paid</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">MK 577.5M</div>
              <div className="text-sm text-gray-500">This quarter</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Net Profit</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">MK 577.5M</div>
              <div className="text-sm text-green-500">+8.2% vs last quarter</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Claim Ratio</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">39%</div>
              <div className="text-sm text-gray-500">Industry avg: 42%</div>
            </CardContent>
          </Card>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Revenue vs Claims Trend</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={revenueData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="premiums" fill="#10b981" name="Premiums" />
                  <Bar dataKey="claims" fill="#ef4444" name="Claims Paid" />
                  <Line type="monotone" dataKey="profit" stroke="#3b82f6" strokeWidth={2} name="Profit" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Expense Breakdown</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={expenseBreakdown}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {expenseBreakdown.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => `MK ${(value as number).toLocaleString()}`} />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Revenue by Policy Type</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={policyRevenue}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="type" />
                  <YAxis />
                  <Tooltip formatter={(value) => `MK ${(value as number).toLocaleString()}`} />
                  <Legend />
                  <Bar dataKey="amount" fill="#3b82f6" name="Revenue" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Claim Ratio Trend</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={claimRatio}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="month" />
                  <YAxis label={{ value: '%', angle: -90, position: 'insideLeft' }} />
                  <Tooltip formatter={(value) => `${value}%`} />
                  <Line type="monotone" dataKey="ratio" stroke="#f59e0b" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
