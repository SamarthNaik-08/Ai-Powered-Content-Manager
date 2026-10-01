import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Typography, Statistic, Space, Spin } from 'antd';
import {
  FileTextOutlined, EditOutlined, EyeOutlined, CheckCircleOutlined,
  ClockCircleOutlined, CloudUploadOutlined, InboxOutlined, CloseCircleOutlined,
  FolderOutlined, TagsOutlined
} from '@ant-design/icons';
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip as RTooltip,
  ResponsiveContainer, Legend
} from 'recharts';
import { contentService } from '@/services/contentService';
import { DashboardStats } from '@/types/content';
import { useAuth } from '@/hooks/useAuth';

const { Title, Text } = Typography;

const STATUS_CHART_COLORS = [
  '#94a3b8', // DRAFT - slate
  '#f59e0b', // IN_REVIEW - amber
  '#06b6d4', // APPROVED - cyan
  '#3b82f6', // SCHEDULED - blue
  '#10b981', // PUBLISHED - emerald
  '#ef4444', // REJECTED - red
  '#6b7280', // ARCHIVED - gray
];

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const data = await contentService.getDashboardStats();
      setStats(data);
    } catch (err: any) {
      // Stats API may not be available yet, show defaults
      setStats({
        totalContent: 0, drafts: 0, inReview: 0, approved: 0,
        published: 0, scheduled: 0, archived: 0, rejected: 0,
        totalCategories: 0, totalTags: 0,
      });
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <Spin size="large" tip="Loading dashboard..." />
      </div>
    );
  }

  const pieData = stats ? [
    { name: 'Drafts', value: stats.drafts },
    { name: 'In Review', value: stats.inReview },
    { name: 'Approved', value: stats.approved },
    { name: 'Scheduled', value: stats.scheduled },
    { name: 'Published', value: stats.published },
    { name: 'Rejected', value: stats.rejected },
    { name: 'Archived', value: stats.archived },
  ].filter(d => d.value > 0) : [];

  const barData = stats ? [
    { name: 'Drafts', count: stats.drafts, fill: '#94a3b8' },
    { name: 'In Review', count: stats.inReview, fill: '#f59e0b' },
    { name: 'Approved', count: stats.approved, fill: '#06b6d4' },
    { name: 'Scheduled', count: stats.scheduled, fill: '#3b82f6' },
    { name: 'Published', count: stats.published, fill: '#10b981' },
    { name: 'Rejected', count: stats.rejected, fill: '#ef4444' },
    { name: 'Archived', count: stats.archived, fill: '#6b7280' },
  ] : [];

  const statCards = [
    { title: 'Total Content', value: stats?.totalContent || 0, icon: <FileTextOutlined />, color: '#2563eb' },
    { title: 'Drafts', value: stats?.drafts || 0, icon: <EditOutlined />, color: '#94a3b8' },
    { title: 'In Review', value: stats?.inReview || 0, icon: <EyeOutlined />, color: '#f59e0b' },
    { title: 'Published', value: stats?.published || 0, icon: <CloudUploadOutlined />, color: '#10b981' },
    { title: 'Scheduled', value: stats?.scheduled || 0, icon: <ClockCircleOutlined />, color: '#3b82f6' },
    { title: 'Archived', value: stats?.archived || 0, icon: <InboxOutlined />, color: '#6b7280' },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ margin: 0, fontWeight: 700 }}>
          Welcome back, {user?.name || 'User'}!
        </Title>
        <Text type="secondary">
          AI-Powered Content Management Platform &bull; Phase 2 Active
        </Text>
      </div>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {statCards.map((card, i) => (
          <Col xs={12} sm={8} lg={4} key={i}>
            <Card bordered={false} style={{
              boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 12,
              borderLeft: `4px solid ${card.color}`,
            }}>
              <Statistic
                title={card.title}
                value={card.value}
                valueStyle={{ color: card.color, fontWeight: 700, fontSize: 28 }}
                prefix={card.icon}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 12 }}>
            <Statistic
              title="Categories"
              value={stats?.totalCategories || 0}
              valueStyle={{ color: '#8b5cf6', fontWeight: 700 }}
              prefix={<FolderOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 12 }}>
            <Statistic
              title="Tags"
              value={stats?.totalTags || 0}
              valueStyle={{ color: '#ec4899', fontWeight: 700 }}
              prefix={<TagsOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 12 }}>
            <Statistic
              title="Rejected"
              value={stats?.rejected || 0}
              valueStyle={{ color: '#ef4444', fontWeight: 700 }}
              prefix={<CloseCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 12 }}>
            <Statistic
              title="Approved"
              value={stats?.approved || 0}
              valueStyle={{ color: '#06b6d4', fontWeight: 700 }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title={<Space><FileTextOutlined style={{ color: '#2563eb' }} /><span>Content by Status</span></Space>}
            style={{ borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.05)', height: '100%' }}
          >
            {pieData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={3}
                    dataKey="value"
                    label={({ name, value }) => `${name}: ${value}`}
                  >
                    {pieData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={STATUS_CHART_COLORS[index % STATUS_CHART_COLORS.length]} />
                    ))}
                  </Pie>
                  <RTooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ textAlign: 'center', padding: 60 }}>
                <Text type="secondary">No content data yet. Create your first content to see charts.</Text>
              </div>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title={<Space><FileTextOutlined style={{ color: '#8b5cf6' }} /><span>Content Distribution</span></Space>}
            style={{ borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.05)', height: '100%' }}
          >
            {barData.some(d => d.count > 0) ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={barData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                  <YAxis allowDecimals={false} />
                  <RTooltip />
                  <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                    {barData.map((entry, index) => (
                      <Cell key={`bar-${index}`} fill={entry.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ textAlign: 'center', padding: 60 }}>
                <Text type="secondary">No content data yet.</Text>
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};
