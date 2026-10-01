import React, { useEffect, useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Input,
  Select,
  Modal,
  Form,
  Checkbox,
  message,
  Typography,
  Popconfirm,
  Tooltip,
  Badge,
} from 'antd';
import {
  SearchOutlined,
  EditOutlined,
  StopOutlined,
  CheckCircleOutlined,
  UserOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { userAdminService } from '@/services/userAdminService';
import { UserManagementItem, UserStatus } from '@/types/enterprise';
import { useAuth } from '@/hooks/useAuth';

const { Title, Text } = Typography;
const { Option } = Select;

const AVAILABLE_ROLES = [
  { label: 'Administrator', value: 'ROLE_ADMIN' },
  { label: 'Editor', value: 'ROLE_EDITOR' },
  { label: 'Author', value: 'ROLE_AUTHOR' },
  { label: 'Viewer', value: 'ROLE_VIEWER' },
];

export const UserManagementPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<UserManagementItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [total, setTotal] = useState<number>(0);
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(10);

  // Filters
  const [search, setSearch] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<UserStatus | undefined>(undefined);
  const [roleFilter, setRoleFilter] = useState<string | undefined>(undefined);

  // Edit Roles Modal
  const [editingUser, setEditingUser] = useState<UserManagementItem | null>(null);
  const [roleModalVisible, setRoleModalVisible] = useState<boolean>(false);
  const [savingRoles, setSavingRoles] = useState<boolean>(false);
  const [form] = Form.useForm();

  const fetchUsers = async (
    currentPage = page,
    currentSearch = search,
    currentStatus = statusFilter,
    currentRole = roleFilter
  ) => {
    setLoading(true);
    try {
      const data = await userAdminService.getUsers({
        search: currentSearch || undefined,
        status: currentStatus,
        role: currentRole,
        page: currentPage,
        size: pageSize,
      });
      setUsers(data.content);
      setTotal(data.totalElements);
    } catch (err: any) {
      message.error(err.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers(page, search, statusFilter, roleFilter);
  }, [page, pageSize]);

  const handleSearch = () => {
    setPage(0);
    fetchUsers(0, search, statusFilter, roleFilter);
  };

  const handleReset = () => {
    setSearch('');
    setStatusFilter(undefined);
    setRoleFilter(undefined);
    setPage(0);
    fetchUsers(0, '', undefined, undefined);
  };

  const handleEditRoles = (user: UserManagementItem) => {
    setEditingUser(user);
    form.setFieldsValue({ roles: user.roles });
    setRoleModalVisible(true);
  };

  const handleSaveRoles = async () => {
    try {
      const values = await form.validateFields();
      if (!editingUser) return;
      setSavingRoles(true);
      await userAdminService.updateRoles(editingUser.id, values.roles);
      message.success(`Roles updated for ${editingUser.name}`);
      setRoleModalVisible(false);
      fetchUsers(page, search, statusFilter, roleFilter);
    } catch (err: any) {
      message.error(err.message || 'Failed to update roles');
    } finally {
      setSavingRoles(false);
    }
  };

  const handleToggleStatus = async (user: UserManagementItem) => {
    const newStatus: UserStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      await userAdminService.updateStatus(user.id, newStatus);
      message.success(`User status updated to ${newStatus}`);
      fetchUsers(page, search, statusFilter, roleFilter);
    } catch (err: any) {
      message.error(err.message || 'Failed to update user status');
    }
  };

  const getStatusBadge = (status: UserStatus) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge status="success" text="Active" />;
      case 'SUSPENDED':
        return <Badge status="error" text="Suspended" />;
      case 'INACTIVE':
        return <Badge status="default" text="Inactive" />;
      default:
        return <Badge status="warning" text={status} />;
    }
  };

  const getRoleColor = (role: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'purple';
      case 'ROLE_EDITOR':
        return 'blue';
      case 'ROLE_AUTHOR':
        return 'green';
      default:
        return 'default';
    }
  };

  const columns: ColumnsType<UserManagementItem> = [
    {
      title: 'User',
      key: 'name',
      render: (_, record) => (
        <Space>
          <UserOutlined style={{ fontSize: 18, color: '#64748b' }} />
          <div>
            <Text strong style={{ display: 'block' }}>
              {record.name}
            </Text>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {record.email}
            </Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: (status: UserStatus) => getStatusBadge(status),
    },
    {
      title: 'Assigned Roles',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles: string[]) => (
        <Space size={4} wrap>
          {roles.map((r) => (
            <Tag color={getRoleColor(r)} key={r}>
              {r.replace('ROLE_', '')}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Joined Date',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    {
      title: 'Last Active',
      dataIndex: 'lastLoginAt',
      key: 'lastLoginAt',
      width: 160,
      render: (date?: string) =>
        date ? new Date(date).toLocaleDateString() : <Text type="secondary">Never</Text>,
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 160,
      render: (_, record) => {
        const isSelf = record.email === currentUser?.email;
        return (
          <Space size={8}>
            <Tooltip title="Modify assigned roles">
              <Button
                type="text"
                icon={<EditOutlined />}
                onClick={() => handleEditRoles(record)}
              />
            </Tooltip>

            {!isSelf && (
              <Popconfirm
                title={record.status === 'ACTIVE' ? 'Suspend User?' : 'Activate User?'}
                description={`Are you sure you want to ${
                  record.status === 'ACTIVE' ? 'suspend' : 'activate'
                } ${record.name}?`}
                onConfirm={() => handleToggleStatus(record)}
                okText="Yes"
                cancelText="No"
              >
                <Tooltip title={record.status === 'ACTIVE' ? 'Suspend User' : 'Activate User'}>
                  <Button
                    type="text"
                    danger={record.status === 'ACTIVE'}
                    icon={
                      record.status === 'ACTIVE' ? (
                        <StopOutlined />
                      ) : (
                        <CheckCircleOutlined style={{ color: '#10b981' }} />
                      )
                    }
                  />
                </Tooltip>
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={3} style={{ margin: 0 }}>
          User Management
        </Title>
        <Text type="secondary">
          Manage system accounts, assign RBAC security roles, and monitor user statuses
        </Text>
      </div>

      <Card style={{ marginBottom: 24 }}>
        <Space size={16} wrap style={{ width: '100%' }}>
          <Input
            placeholder="Search name or email..."
            prefix={<SearchOutlined />}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 260 }}
          />

          <Select
            placeholder="Filter by Status"
            allowClear
            value={statusFilter}
            onChange={(val) => setStatusFilter(val)}
            style={{ width: 170 }}
          >
            <Option value="ACTIVE">Active</Option>
            <Option value="SUSPENDED">Suspended</Option>
            <Option value="INACTIVE">Inactive</Option>
          </Select>

          <Select
            placeholder="Filter by Role"
            allowClear
            value={roleFilter}
            onChange={(val) => setRoleFilter(val)}
            style={{ width: 180 }}
          >
            <Option value="ROLE_ADMIN">Administrator</Option>
            <Option value="ROLE_EDITOR">Editor</Option>
            <Option value="ROLE_AUTHOR">Author</Option>
            <Option value="ROLE_VIEWER">Viewer</Option>
          </Select>

          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            Filter
          </Button>

          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            Reset
          </Button>
        </Space>
      </Card>

      <Card bodyStyle={{ padding: 0 }}>
        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize,
            total,
            onChange: (p, size) => {
              setPage(p - 1);
              setPageSize(size);
            },
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
          }}
        />
      </Card>

      {/* Role Assignment Modal */}
      <Modal
        title={`Edit Roles for ${editingUser?.name}`}
        open={roleModalVisible}
        onOk={handleSaveRoles}
        confirmLoading={savingRoles}
        onCancel={() => setRoleModalVisible(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="roles"
            label="Assigned System Roles"
            rules={[{ required: true, message: 'Please select at least one role' }]}
          >
            <Checkbox.Group options={AVAILABLE_ROLES} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
