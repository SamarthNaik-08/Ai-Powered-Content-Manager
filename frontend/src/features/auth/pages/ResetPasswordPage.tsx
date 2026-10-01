import React from 'react';
import { Link } from 'react-router-dom';
import { Card, Result, Button } from 'antd';
import { ArrowLeftOutlined, KeyOutlined } from '@ant-design/icons';

export const ResetPasswordPage: React.FC = () => {
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
          icon={<KeyOutlined style={{ color: '#8b5cf6', fontSize: 48 }} />}
          title="Reset Password"
          subTitle="Password reset token verification UI placeholder. Token-based resets will be activated with the notification/email dispatch service in Phase 4."
          extra={[
            <Link to="/login" key="login">
              <Button type="primary" icon={<ArrowLeftOutlined />}>
                Return to Login
              </Button>
            </Link>,
          ]}
        />
      </Card>
    </div>
  );
};
