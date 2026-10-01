import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Card, Input, Button, Alert, Typography, Divider } from 'antd';
import { LockOutlined, MailOutlined, UserOutlined, UserAddOutlined } from '@ant-design/icons';
import { useAuth } from '@/hooks/useAuth';

const { Title, Text } = Typography;

const registerSchema = z
  .object({
    name: z.string().min(2, 'Name must be at least 2 characters').max(100, 'Name cannot exceed 100 characters'),
    email: z.string().email('Please enter a valid email address'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(64, 'Password cannot exceed 64 characters'),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register: registerUser } = useAuth();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      name: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  });

  const onSubmit = async (data: RegisterFormData) => {
    setErrorMessage(null);
    setIsSubmitting(true);
    try {
      await registerUser({
        name: data.name,
        email: data.email,
        password: data.password,
      });
      navigate('/dashboard', { replace: true });
    } catch (err: any) {
      setErrorMessage(
        err.response?.data?.message || err.message || 'Registration failed. Please check details and try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
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
          maxWidth: 460,
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
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
              borderRadius: 12,
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: 16,
            }}
          >
            <UserAddOutlined style={{ fontSize: 28, color: '#ffffff' }} />
          </div>
          <Title level={3} style={{ margin: 0, fontWeight: 700 }}>
            Create Account
          </Title>
          <Text type="secondary">Join the AI-Powered Content Management Platform</Text>
        </div>

        {errorMessage && (
          <Alert
            message="Registration Error"
            description={errorMessage}
            type="error"
            showIcon
            closable
            style={{ marginBottom: 20 }}
          />
        )}

        <form onSubmit={handleSubmit(onSubmit)}>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Full Name</label>
            <Controller
              name="name"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  size="large"
                  placeholder="Jane Doe"
                  prefix={<UserOutlined style={{ color: '#94a3b8' }} />}
                  status={errors.name ? 'error' : ''}
                />
              )}
            />
            {errors.name && (
              <Text type="danger" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
                {errors.name.message}
              </Text>
            )}
          </div>

          <div style={{ marginBottom: 16 }}>
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

          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Password</label>
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <Input.Password
                  {...field}
                  size="large"
                  placeholder="Minimum 8 characters"
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

          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', marginBottom: 6, fontWeight: 500 }}>Confirm Password</label>
            <Controller
              name="confirmPassword"
              control={control}
              render={({ field }) => (
                <Input.Password
                  {...field}
                  size="large"
                  placeholder="Re-enter password"
                  prefix={<LockOutlined style={{ color: '#94a3b8' }} />}
                  status={errors.confirmPassword ? 'error' : ''}
                />
              )}
            />
            {errors.confirmPassword && (
              <Text type="danger" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
                {errors.confirmPassword.message}
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
              height: 46,
              fontWeight: 600,
              background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
              borderColor: '#10b981',
            }}
          >
            Create Free Account
          </Button>
        </form>

        <Divider style={{ margin: '20px 0' }} />

        <div style={{ textAlign: 'center' }}>
          <Text type="secondary" style={{ fontSize: 13 }}>
            Already have an account?{' '}
            <Link to="/login" style={{ fontWeight: 600 }}>
              Sign In
            </Link>
          </Text>
        </div>
      </Card>
    </div>
  );
};
