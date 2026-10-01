import React, { useEffect, useState } from 'react';
import {
  Card,
  Row,
  Col,
  Statistic,
  Typography,
  Tabs,
  Spin,
  Button,
  Space,
  Tag,
  Progress,
  message,
} from 'antd';
import {
  FileTextOutlined,
  SendOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  FieldTimeOutlined,
  ReloadOutlined,
  LineChartOutlined,
} from '@ant-design/icons';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
} from 'recharts';
import { analyticsService } from '@/services/analyticsService';
import { AnalyticsDashboardData, AIAnalyticsData } from '@/types/enterprise';

const { Title, Text } = Typography;

export const AnalyticsPage: React.FC = () => {
  const [dashboardData, setDashboardData] = useState<AnalyticsDashboardData | null>(null);
  const [aiData, setAiData] = useState<AIAnalyticsData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchAnalytics = async () => {
    setLoading(true);
    try {
      const [dash, ai] = await Promise.all([
        analyticsService.getDashboardAnalytics(),
        analyticsService.getAIAnalytics(),
      ]);
      setDashboardData(dash);
      setAiData(ai);
    } catch (err: any) {
      message.error(err.message || 'Failed to load analytics data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const formatTokens = (tokens: number) => {
    if (tokens >= 1000000) return (tokens / 1000000).toFixed(2) + 'M';
    if (tokens >= 1000) return (tokens / 1000).toFixed(1) + 'k';
    return tokens.toString();
  };

  const getSuccessRate = () => {
    if (!aiData || aiData.totalRequests === 0) return 100;
    return Math.round((aiData.successfulRequests / aiData.totalRequests) * 100);
  };

  // Prepare trend data combining creation & publishing
  const trendChartData = (dashboardData?.creationTrend || []).map((c, i) => ({
    date: c.date.slice(5), // MM-DD
    Created: c.count,
    Published: dashboardData?.publishingTrend[i]?.count || 0,
  }));

  const aiOpsData = Object.entries(aiData?.byOperation || {}).map(([op, count]) => ({
    operation: op,
    count,
  }));

  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 24,
          flexWrap: 'wrap',
          gap: 16,
        }}
      >
        <div>
          <Title level={3} style={{ margin: 0 }}>
            Platform Analytics & Intelligence
          </Title>
          <Text type="secondary">
            Performance metrics, publication velocity, and AI telemetry cached for efficiency
          </Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={fetchAnalytics} loading={loading}>
          Refresh Metrics
        </Button>
      </div>

      {loading && !dashboardData ? (
        <div style={{ textAlign: 'center', padding: '100px 0' }}>
          <Spin size="large" />
        </div>
      ) : (
        <Tabs
          defaultActiveKey="content"
          items={[
            {
              key: 'content',
              label: (
                <span>
                  <LineChartOutlined /> Content Analytics
                </span>
              ),
              children: (
                <div>
                  {/* Top Stats Cards */}
                  <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                    <Col xs={12} sm={8} md={4}>
                      <Card bordered={false} style={{ background: '#f0fdf4' }}>
                        <Statistic
                          title="Published"
                          value={dashboardData?.counts.published || 0}
                          valueStyle={{ color: '#16a34a' }}
                          prefix={<SendOutlined />}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={8} md={4}>
                      <Card bordered={false} style={{ background: '#eff6ff' }}>
                        <Statistic
                          title="Total Content"
                          value={dashboardData?.counts.total || 0}
                          valueStyle={{ color: '#2563eb' }}
                          prefix={<FileTextOutlined />}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={8} md={4}>
                      <Card bordered={false} style={{ background: '#fffbeb' }}>
                        <Statistic
                          title="In Review"
                          value={dashboardData?.counts.inReview || 0}
                          valueStyle={{ color: '#d97706' }}
                          prefix={<ClockCircleOutlined />}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={8} md={4}>
                      <Card bordered={false} style={{ background: '#fdf4ff' }}>
                        <Statistic
                          title="Approved"
                          value={dashboardData?.counts.approved || 0}
                          valueStyle={{ color: '#9333ea' }}
                          prefix={<CheckCircleOutlined />}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={8} md={4}>
                      <Card bordered={false} style={{ background: '#ecfeff' }}>
                        <Statistic
                          title="Scheduled"
                          value={dashboardData?.counts.scheduled || 0}
                          valueStyle={{ color: '#0891b2' }}
                          prefix={<ClockCircleOutlined />}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={8} md={4}>
                      <Card bordered={false} style={{ background: '#f8fafc' }}>
                        <Statistic
                          title="Drafts"
                          value={dashboardData?.counts.draft || 0}
                          valueStyle={{ color: '#475569' }}
                        />
                      </Card>
                    </Col>
                  </Row>

                  {/* Velocity Timeline */}
                  <Card
                    title="Content Creation vs. Publication Velocity (Past 14 Days)"
                    style={{ marginBottom: 24 }}
                  >
                    <div style={{ height: 320, width: '100%' }}>
                      <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={trendChartData}>
                          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                          <XAxis dataKey="date" stroke="#94a3b8" />
                          <YAxis stroke="#94a3b8" allowDecimals={false} />
                          <RechartsTooltip />
                          <Legend />
                          <Area
                            type="monotone"
                            dataKey="Created"
                            stroke="#3b82f6"
                            fill="#93c5fd"
                            fillOpacity={0.3}
                          />
                          <Area
                            type="monotone"
                            dataKey="Published"
                            stroke="#10b981"
                            fill="#86efac"
                            fillOpacity={0.3}
                          />
                        </AreaChart>
                      </ResponsiveContainer>
                    </div>
                  </Card>

                  {/* Distribution Cards */}
                  <Row gutter={[20, 20]}>
                    <Col xs={24} md={8}>
                      <Card title="Content by Category" style={{ height: '100%' }}>
                        {(dashboardData?.byCategory || []).length === 0 ? (
                          <Text type="secondary">No categorized content yet</Text>
                        ) : (
                          dashboardData?.byCategory.map((cat) => {
                            const pct = Math.round(
                              (cat.count / (dashboardData.counts.total || 1)) * 100
                            );
                            return (
                              <div key={cat.name} style={{ marginBottom: 16 }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                  <Text strong>{cat.name}</Text>
                                  <Text type="secondary">{cat.count} items</Text>
                                </div>
                                <Progress percent={pct} strokeColor="#3b82f6" size="small" />
                              </div>
                            );
                          })
                        )}
                      </Card>
                    </Col>

                    <Col xs={24} md={8}>
                      <Card title="Content by Format / Type" style={{ height: '100%' }}>
                        {(dashboardData?.byType || []).length === 0 ? (
                          <Text type="secondary">No content recorded yet</Text>
                        ) : (
                          dashboardData?.byType.map((t) => {
                            const pct = Math.round(
                              (t.count / (dashboardData.counts.total || 1)) * 100
                            );
                            return (
                              <div key={t.name} style={{ marginBottom: 16 }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                  <Text strong>{t.name.replace('_', ' ')}</Text>
                                  <Text type="secondary">{t.count} items</Text>
                                </div>
                                <Progress percent={pct} strokeColor="#8b5cf6" size="small" />
                              </div>
                            );
                          })
                        )}
                      </Card>
                    </Col>

                    <Col xs={24} md={8}>
                      <Card title="Top Contributing Authors" style={{ height: '100%' }}>
                        {(dashboardData?.byAuthor || []).length === 0 ? (
                          <Text type="secondary">No author contributions yet</Text>
                        ) : (
                          dashboardData?.byAuthor.map((a) => {
                            const pct = Math.round(
                              (a.count / (dashboardData.counts.total || 1)) * 100
                            );
                            return (
                              <div key={a.name} style={{ marginBottom: 16 }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                  <Text strong>{a.name}</Text>
                                  <Text type="secondary">{a.count} posts</Text>
                                </div>
                                <Progress percent={pct} strokeColor="#10b981" size="small" />
                              </div>
                            );
                          })
                        )}
                      </Card>
                    </Col>
                  </Row>
                </div>
              ),
            },
            {
              key: 'ai',
              label: (
                <span>
                  <RobotOutlined /> AI Telemetry & Usage
                </span>
              ),
              children: (
                <div>
                  <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                    <Col xs={12} sm={6}>
                      <Card bordered={false} style={{ background: '#f5f3ff' }}>
                        <Statistic
                          title="Total AI Requests"
                          value={aiData?.totalRequests || 0}
                          valueStyle={{ color: '#7c3aed' }}
                          prefix={<RobotOutlined />}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={6}>
                      <Card bordered={false} style={{ background: '#f0fdf4' }}>
                        <Statistic
                          title="Success Rate"
                          value={getSuccessRate()}
                          suffix="%"
                          valueStyle={{ color: '#16a34a' }}
                          prefix={<CheckCircleOutlined />}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={6}>
                      <Card bordered={false} style={{ background: '#eff6ff' }}>
                        <Statistic
                          title="Total Tokens Used"
                          value={formatTokens(aiData?.totalTokens || 0)}
                          valueStyle={{ color: '#2563eb' }}
                          prefix={<ThunderboltOutlined />}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={6}>
                      <Card bordered={false} style={{ background: '#fff7ed' }}>
                        <Statistic
                          title="Avg. Latency"
                          value={aiData?.averageExecutionTimeMs || 0}
                          suffix="ms"
                          valueStyle={{ color: '#ea580c' }}
                          prefix={<FieldTimeOutlined />}
                        />
                      </Card>
                    </Col>
                  </Row>

                  <Row gutter={[20, 20]}>
                    <Col xs={24} md={14}>
                      <Card title="Requests by AI Operation" style={{ height: '100%' }}>
                        <div style={{ height: 300, width: '100%' }}>
                          <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={aiOpsData}>
                              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                              <XAxis dataKey="operation" stroke="#94a3b8" />
                              <YAxis stroke="#94a3b8" allowDecimals={false} />
                              <RechartsTooltip />
                              <Bar dataKey="count" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
                            </BarChart>
                          </ResponsiveContainer>
                        </div>
                      </Card>
                    </Col>

                    <Col xs={24} md={10}>
                      <Card title="Active Model & Providers" style={{ height: '100%' }}>
                        <div style={{ marginBottom: 20 }}>
                          <Text strong style={{ display: 'block', marginBottom: 8 }}>
                            Configured Providers:
                          </Text>
                          <Space wrap>
                            {Object.entries(aiData?.byProvider || {}).map(([p, c]) => (
                              <Tag color="geekblue" key={p} style={{ padding: '4px 10px', fontSize: 13 }}>
                                {p}: <strong>{c} calls</strong>
                              </Tag>
                            ))}
                          </Space>
                        </div>

                        <div>
                          <Text strong style={{ display: 'block', marginBottom: 8 }}>
                            LLM Models Deployed:
                          </Text>
                          <Space direction="vertical" style={{ width: '100%' }}>
                            {Object.entries(aiData?.byModel || {}).map(([m, c]) => (
                              <div
                                key={m}
                                style={{
                                  display: 'flex',
                                  justifyContent: 'space-between',
                                  padding: '8px 12px',
                                  backgroundColor: '#f8fafc',
                                  borderRadius: 8,
                                }}
                              >
                                <code>{m}</code>
                                <Tag color="purple">{c} requests</Tag>
                              </div>
                            ))}
                          </Space>
                        </div>
                      </Card>
                    </Col>
                  </Row>
                </div>
              ),
            },
          ]}
        />
      )}
    </div>
  );
};
