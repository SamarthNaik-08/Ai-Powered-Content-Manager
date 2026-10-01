import React, { useEffect, useState } from 'react';
import {
  Badge,
  Button,
  Dropdown,
  List,
  Typography,
  Space,
  Empty,
  Spin,
  Tag,
} from 'antd';
import {
  BellOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  SendOutlined,
  InfoCircleOutlined,
  CheckOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { notificationService } from '@/services/notificationService';
import { NotificationItem, NotificationType } from '@/types/enterprise';

const { Text } = Typography;

export const NotificationDropdown: React.FC = () => {
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [open, setOpen] = useState<boolean>(false);
  const navigate = useNavigate();

  const fetchUnreadCount = async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
    } catch {
      // Ignore background poll errors
    }
  };

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationService.getNotifications(0, 8);
      setNotifications(data.content);
      const unread = data.content.filter((n: NotificationItem) => !n.read).length;
      setUnreadCount(unread);
    } catch {
      // Ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const timer = setInterval(fetchUnreadCount, 30000);
    return () => clearInterval(timer);
  }, []);

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (nextOpen) {
      fetchNotifications();
    }
  };

  const handleItemClick = async (item: NotificationItem) => {
    if (!item.read) {
      try {
        await notificationService.markAsRead(item.id);
        setNotifications((prev) =>
          prev.map((n) => (n.id === item.id ? { ...n, read: true } : n))
        );
        setUnreadCount((c) => Math.max(0, c - 1));
      } catch {
        // Continue navigation
      }
    }
    if (item.link) {
      setOpen(false);
      navigate(item.link);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // Ignore
    }
  };

  const getIcon = (type: NotificationType) => {
    switch (type) {
      case 'CONTENT_APPROVED':
        return <CheckCircleOutlined style={{ color: '#10b981', fontSize: 16 }} />;
      case 'CONTENT_REJECTED':
        return <CloseCircleOutlined style={{ color: '#ef4444', fontSize: 16 }} />;
      case 'CONTENT_PUBLISHED':
      case 'SCHEDULED_PUBLISHED':
        return <SendOutlined style={{ color: '#3b82f6', fontSize: 16 }} />;
      case 'REVIEW_REQUESTED':
        return <ClockCircleOutlined style={{ color: '#f59e0b', fontSize: 16 }} />;
      default:
        return <InfoCircleOutlined style={{ color: '#6b7280', fontSize: 16 }} />;
    }
  };

  const dropdownContent = (
    <div
      style={{
        width: 360,
        backgroundColor: '#fff',
        borderRadius: 12,
        boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
        border: '1px solid #f1f5f9',
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '12px 16px',
          borderBottom: '1px solid #f1f5f9',
          backgroundColor: '#f8fafc',
        }}
      >
        <Space>
          <Text strong style={{ fontSize: 14 }}>
            Notifications
          </Text>
          {unreadCount > 0 && <Tag color="blue">{unreadCount} new</Tag>}
        </Space>
        {unreadCount > 0 && (
          <Button
            type="link"
            size="small"
            icon={<CheckOutlined />}
            onClick={handleMarkAllRead}
            style={{ fontSize: 12, padding: 0 }}
          >
            Mark all read
          </Button>
        )}
      </div>

      <div style={{ maxHeight: 380, overflowY: 'auto', padding: '4px 0' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '30px 0' }}>
            <Spin size="small" />
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ padding: '30px 0' }}>
            <Empty description="No notifications" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          </div>
        ) : (
          <List
            itemLayout="horizontal"
            dataSource={notifications}
            renderItem={(item) => (
              <List.Item
                onClick={() => handleItemClick(item)}
                style={{
                  padding: '10px 16px',
                  cursor: 'pointer',
                  backgroundColor: item.read ? '#fff' : '#f0f9ff',
                  transition: 'background-color 0.2s',
                  borderBottom: '1px solid #f8fafc',
                }}
              >
                <List.Item.Meta
                  avatar={getIcon(item.type)}
                  title={
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Text
                        strong={!item.read}
                        style={{ fontSize: 13, color: item.read ? '#4b5563' : '#1e293b' }}
                      >
                        {item.title}
                      </Text>
                      {!item.read && (
                        <span
                          style={{
                            width: 7,
                            height: 7,
                            borderRadius: '50%',
                            backgroundColor: '#2563eb',
                            display: 'inline-block',
                            marginTop: 6,
                          }}
                        />
                      )}
                    </div>
                  }
                  description={
                    <div>
                      <div
                        style={{
                          fontSize: 12,
                          color: '#64748b',
                          lineHeight: 1.4,
                          marginBottom: 4,
                        }}
                      >
                        {item.message}
                      </div>
                      <Text type="secondary" style={{ fontSize: 11 }}>
                        {new Date(item.createdAt).toLocaleDateString()} at{' '}
                        {new Date(item.createdAt).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </Text>
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </div>
    </div>
  );

  return (
    <Dropdown
      dropdownRender={() => dropdownContent}
      trigger={['click']}
      open={open}
      onOpenChange={handleOpenChange}
      placement="bottomRight"
    >
      <Badge count={unreadCount} overflowCount={99} size="small" offset={[-2, 4]}>
        <Button
          type="text"
          shape="circle"
          icon={<BellOutlined style={{ fontSize: 18 }} />}
          style={{ width: 38, height: 38 }}
        />
      </Badge>
    </Dropdown>
  );
};
