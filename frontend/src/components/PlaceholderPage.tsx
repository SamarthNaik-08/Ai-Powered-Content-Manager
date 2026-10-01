import React from 'react';
import { Card, Typography, Tag, Space, Breadcrumb } from 'antd';
import { ClockCircleOutlined, ThunderboltOutlined, HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

const { Title, Paragraph, Text } = Typography;

interface PlaceholderPageProps {
  moduleName: string;
  targetPhase: string;
  description: string;
  features: string[];
}

export const PlaceholderPage: React.FC<PlaceholderPageProps> = ({
  moduleName,
  targetPhase,
  description,
  features,
}) => {
  return (
    <div>
      <Breadcrumb
        style={{ marginBottom: 20 }}
        items={[
          {
            title: (
              <Link to="/dashboard">
                <HomeOutlined /> Dashboard
              </Link>
            ),
          },
          { title: moduleName },
        ]}
      />

      <Card
        style={{
          borderRadius: 12,
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <Title level={3} style={{ margin: 0 }}>
            {moduleName}
          </Title>
          <Tag color="processing" icon={<ClockCircleOutlined />} style={{ padding: '4px 10px', fontSize: 13 }}>
            Scheduled for {targetPhase}
          </Tag>
        </div>

        <Paragraph type="secondary" style={{ fontSize: 15, maxWidth: 800 }}>
          {description}
        </Paragraph>

        <div style={{ marginTop: 24, padding: 20, background: '#f8fafc', borderRadius: 8, border: '1px solid #e2e8f0' }}>
          <Text strong style={{ display: 'block', marginBottom: 12, color: '#334155' }}>
            <ThunderboltOutlined style={{ color: '#2563eb', marginRight: 8 }} />
            Planned Module Capabilities:
          </Text>
          <Space direction="vertical" size={8}>
            {features.map((feature, idx) => (
              <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{ width: 6, height: 6, borderRadius: '50%', background: '#3b82f6' }} />
                <Text>{feature}</Text>
              </div>
            ))}
          </Space>
        </div>
      </Card>
    </div>
  );
};
