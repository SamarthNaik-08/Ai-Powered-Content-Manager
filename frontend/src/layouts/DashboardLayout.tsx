import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Layout,
  Menu,
  Avatar,
  Dropdown,
  Space,
  Typography,
  Tag,
  Button,
  theme,
} from 'antd';
import {
  DashboardOutlined,
  FileTextOutlined,
  RobotOutlined,
  PictureOutlined,
  FolderOutlined,
  TagsOutlined,
  LineChartOutlined,
  TeamOutlined,
  HistoryOutlined,
  SettingOutlined,
  LogoutOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { useAuth } from '@/hooks/useAuth';
import { NotificationDropdown } from '@/components/NotificationDropdown';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export const DashboardLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'user-header',
      label: (
        <div style={{ padding: '4px 0' }}>
          <Text strong style={{ display: 'block' }}>
            {user?.name}
          </Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {user?.email}
          </Text>
          <div style={{ marginTop: 6 }}>
            {user?.roles?.map((r) => (
              <Tag color="blue" key={r} style={{ fontSize: 11 }}>
                {r}
              </Tag>
            ))}
          </div>
        </div>
      ),
      disabled: true,
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Sign Out',
      danger: true,
      onClick: handleLogout,
    },
  ];

  const menuItems: MenuProps['items'] = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: 'Dashboard',
    },
    {
      type: 'divider',
    },
    {
      key: '/content',
      icon: <FileTextOutlined />,
      label: 'Content',
    },
    {
      key: '/ai-assistant',
      icon: <RobotOutlined />,
      label: 'AI Assistant',
    },
    {
      key: '/media',
      icon: <PictureOutlined />,
      label: 'Media Library',
    },
    {
      key: '/categories',
      icon: <FolderOutlined />,
      label: 'Categories',
    },
    {
      key: '/tags',
      icon: <TagsOutlined />,
      label: 'Tags',
    },
    {
      type: 'divider',
    },
    {
      key: '/analytics',
      icon: <LineChartOutlined />,
      label: 'Analytics',
    },
    {
      key: '/users',
      icon: <TeamOutlined />,
      label: 'Users',
    },
    {
      key: '/audit-logs',
      icon: <HistoryOutlined />,
      label: 'Audit Logs',
    },
    {
      key: '/settings',
      icon: <SettingOutlined />,
      label: 'Settings',
    },
  ];

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(key);
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={240}
        theme="dark"
        style={{
          boxShadow: '2px 0 8px 0 rgba(29, 35, 41, 0.05)',
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            padding: '0 20px',
            gap: 12,
            borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
          }}
        >
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 8,
              background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <SafetyCertificateOutlined style={{ color: '#fff', fontSize: 20 }} />
          </div>
          {!collapsed && (
            <span
              style={{
                color: '#fff',
                fontWeight: 700,
                fontSize: 15,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              Content Manager
            </span>
          )}
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ borderRight: 0, marginTop: 12 }}
        />
      </Sider>

      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #f0f0f0',
            height: 64,
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{ fontSize: 16, width: 44, height: 44 }}
          />

          <Space size={20}>
            <NotificationDropdown />

            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" arrow>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar
                  style={{
                    backgroundColor: '#2563eb',
                    verticalAlign: 'middle',
                  }}
                  icon={<UserOutlined />}
                >
                  {user?.name?.charAt(0).toUpperCase()}
                </Avatar>
                <div style={{ lineHeight: 1.2, display: 'flex', flexDirection: 'column' }}>
                  <Text strong style={{ fontSize: 13 }}>
                    {user?.name}
                  </Text>
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    {user?.roles?.[0] || 'Authenticated'}
                  </Text>
                </div>
              </Space>
            </Dropdown>
          </Space>
        </Header>

        <Content
          style={{
            margin: '24px 24px',
            padding: 24,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            minHeight: 280,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};
