import React from 'react';
import { Link } from 'react-router-dom';
import { Card, Result, Button } from 'antd';
import { ArrowLeftOutlined, MailOutlined } from '@ant-design/icons';

export const ForgotPasswordPage: React.FC = () => {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
        padding: '24px',
      }}
    >
      <Card
        style={{
          width: '100%',
          maxWidth: 460,
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
          borderRadius: 16,
          border: '1px solid rgba(255, 255, 255, 0.1)',
        }}
      >
        <Result
          icon={<MailOutlined style={{ color: '#3b82f6', fontSize: 48 }} />}
          title="Password Recovery"
          subTitle="Self-service email password recovery is scheduled for Phase 4 (Enterprise Features). In Phase 1, password reset is administered via RBAC by system administrators."
          extra={[
            <Link to="/login" key="login">
              <Button type="primary" icon={<ArrowLeftOutlined />}>
                Back to Sign In
              </Button>
            </Link>,
          ]}
        />
      </Card>
    </div>
  );
};
