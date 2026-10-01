import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Card, Input, Button, Alert, Typography, Divider } from 'antd';
import { LockOutlined, MailOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useAuth } from '@/hooks/useAuth';

const { Title, Text, Paragraph } = Typography;

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const from = (location.state as any)?.from?.pathname || '/dashboard';

  const {
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setErrorMessage(null);
    setIsSubmitting(true);
    try {
      await login(data);
      navigate(from, { replace: true });
    } catch (err: any) {
      setErrorMessage(
        err.response?.data?.message || err.message || 'Invalid email or password. Please try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleFillDemoAdmin = () => {
    setValue('email', 'admin@contentmanager.com');
    setValue('password', 'Admin@123456');
  };

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
          maxWidth: 440,
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5)',
          borderRadius: 16,
          border: '1px solid rgba(255, 255, 255, 0.1)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div
            style={{
              width: 56,
              height: 56,
              background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
              borderRadius: 12,
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 16,
            }}
          >
            <SafetyCertificateOutlined style={{ fontSize: 28, color: '#ffffff' }} />
          </div>
          <Title level={3} style={{ margin: 0, fontWeight: 700 }}>
            Sign In
          </Title>
          <Text type="secondary">AI-Powered Content Management Platform</Text>
        </div>

        {errorMessage && (
          <Alert
            message="Authentication Error"
            description={errorMessage}
            type="error"
            showIcon
            closable
            style={{ marginBottom: 20 }}
          />
        )}

        <form onSubmit={handleSubmit(onSubmit)}>
          <div style={{ marginBottom: 18 }}>
            <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Email Address</label>
            <Controller
              name="email"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  size="large"
                  placeholder="name@company.com"
                  prefix={<MailOutlined style={{ color: '#94a3b8' }} />}
                  status={errors.email ? 'error' : ''}
                />
              )}
            />
            {errors.email && (
              <Text type="danger" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
                {errors.email.message}
              </Text>
            )}
          </div>

          <div style={{ marginBottom: 18 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
              <label style={{ fontWeight: 500 }}>Password</label>
              <Link to="/forgot-password" style={{ fontSize: 12 }}>
                Forgot Password?
              </Link>
            </div>
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <Input.Password
                  {...field}
                  size="large"
                  placeholder="Enter your password"
                  prefix={<LockOutlined style={{ color: '#94a3b8' }} />}
                  status={errors.password ? 'error' : ''}
                />
              )}
            />
            {errors.password && (
              <Text type="danger" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
                {errors.password.message}
              </Text>
            )}
          </div>

          <Button
            type="primary"
            htmlType="submit"
            size="large"
            block
            loading={isSubmitting}
            style={{
              marginTop: 10,
              height: 46,
              fontWeight: 600,
              background: 'linear-gradient(135deg, #2563eb 0%, #4f46e5 100%)',
              borderColor: '#2563eb',
            }}
          >
            Sign In to Account
          </Button>
        </form>

        <Divider style={{ margin: '20px 0' }} />

        <div style={{ textAlign: 'center' }}>
          <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 12 }}>
            Demo Account Credentials:
          </Paragraph>
          <Button
            size="small"
            onClick={handleFillDemoAdmin}
            style={{ fontSize: 12, marginBottom: 16 }}
          >
            Use Default Admin (admin@contentmanager.com)
          </Button>

          <div>
            <Text type="secondary" style={{ fontSize: 13 }}>
              Don't have an account yet?{' '}
              <Link to="/register" style={{ fontWeight: 600 }}>
                Sign Up
              </Link>
            </Text>
          </div>
        </div>
      </Card>
    </div>
  );
};
