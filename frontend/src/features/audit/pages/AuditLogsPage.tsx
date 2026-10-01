import React, { useEffect, useState } from 'react';
import {
  Card,
  Table,
  Tag,
  Space,
  Input,
  Select,
  DatePicker,
  Button,
  Typography,
  message,
  Tooltip,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  AuditOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { auditService } from '@/services/auditService';
import { AuditAction, AuditLogItem } from '@/types/enterprise';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const ACTION_OPTIONS: AuditAction[] = [
  'LOGIN',
  'LOGOUT',
  'CREATE_CONTENT',
  'UPDATE_CONTENT',
  'DELETE_CONTENT',
  'SUBMIT_CONTENT',
  'APPROVE_CONTENT',
  'REJECT_CONTENT',
  'PUBLISH_CONTENT',
  'SCHEDULE_CONTENT',
  'RESTORE_VERSION',
  'AI_GENERATION',
  'USER_CREATED',
  'ROLE_CHANGED',
  'USER_STATUS_UPDATED',
  'MEDIA_UPLOAD',
  'MEDIA_DELETE',
];

export const AuditLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [total, setTotal] = useState<number>(0);
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);

  // Filters
  const [actorEmail, setActorEmail] = useState<string>('');
  const [actionFilter, setActionFilter] = useState<string | undefined>(undefined);
  const [resourceFilter, setResourceFilter] = useState<string>('');
  const [dateRange, setDateRange] = useState<[any, any] | null>(null);

  const fetchLogs = async (
    currentPage = page,
    currentActor = actorEmail,
    currentAction = actionFilter,
    currentResource = resourceFilter,
    currentDates = dateRange
  ) => {
    setLoading(true);
    try {
      const from = currentDates && currentDates[0] ? currentDates[0].toISOString() : undefined;
      const to = currentDates && currentDates[1] ? currentDates[1].toISOString() : undefined;

      const data = await auditService.getAuditLogs({
        actorEmail: currentActor || undefined,
        action: currentAction || undefined,
        resource: currentResource || undefined,
        from,
        to,
        page: currentPage,
        size: pageSize,
      });

      setLogs(data.content);
      setTotal(data.totalElements);
    } catch (err: any) {
      message.error(err.message || 'Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs(page, actorEmail, actionFilter, resourceFilter, dateRange);
  }, [page, pageSize]);

  const handleSearch = () => {
    setPage(0);
    fetchLogs(0, actorEmail, actionFilter, resourceFilter, dateRange);
  };

  const handleReset = () => {
    setActorEmail('');
    setActionFilter(undefined);
    setResourceFilter('');
    setDateRange(null);
    setPage(0);
    fetchLogs(0, '', undefined, '', null);
  };

  const getActionColor = (action: AuditAction) => {
    switch (action) {
      case 'PUBLISH_CONTENT':
      case 'APPROVE_CONTENT':
        return 'green';
      case 'REJECT_CONTENT':
      case 'DELETE_CONTENT':
      case 'MEDIA_DELETE':
        return 'red';
      case 'CREATE_CONTENT':
      case 'UPDATE_CONTENT':
      case 'MEDIA_UPLOAD':
        return 'blue';
      case 'AI_GENERATION':
        return 'purple';
      case 'SCHEDULE_CONTENT':
        return 'cyan';
      case 'ROLE_CHANGED':
      case 'USER_STATUS_UPDATED':
        return 'gold';
      case 'LOGIN':
      case 'LOGOUT':
        return 'geekblue';
      default:
        return 'default';
    }
  };

  const columns: ColumnsType<AuditLogItem> = [
    {
      title: 'Timestamp',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (date: string) => (
        <Text style={{ fontSize: 12 }}>
          {new Date(date).toLocaleDateString()} {new Date(date).toLocaleTimeString()}
        </Text>
      ),
    },
    {
      title: 'Actor',
      dataIndex: 'actorEmail',
      key: 'actorEmail',
      width: 220,
      render: (email: string) => (
        <Tooltip title={email}>
          <Text strong style={{ fontSize: 13 }}>
            {email}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      width: 180,
      render: (action: AuditAction) => (
        <Tag color={getActionColor(action)} style={{ fontWeight: 600 }}>
          {action}
        </Tag>
      ),
    },
    {
      title: 'Target Resource',
      key: 'target',
      width: 180,
      render: (_, record) => (
        <Text style={{ fontSize: 12 }}>
          {record.resource ? `${record.resource} #${record.resourceId || 'N/A'}` : '—'}
        </Text>
      ),
    },
    {
      title: 'Origin',
      key: 'origin',
      width: 160,
      render: (_, record) => (
        <Text type="secondary" style={{ fontSize: 11 }}>
          {record.ipAddress || '127.0.0.1'}
        </Text>
      ),
    },
    {
      title: 'Metadata / Context',
      dataIndex: 'metadata',
      key: 'metadata',
      render: (meta?: string) => {
        if (!meta) return <Text type="secondary">—</Text>;
        try {
          const parsed = JSON.parse(meta);
          return (
            <code
              style={{
                fontSize: 11,
                backgroundColor: '#f8fafc',
                padding: '2px 6px',
                borderRadius: 4,
                display: 'inline-block',
                maxWidth: 320,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {JSON.stringify(parsed)}
            </code>
          );
        } catch {
          return (
            <Text
              style={{
                fontSize: 12,
                maxWidth: 320,
                display: 'inline-block',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {meta}
            </Text>
          );
        }
      },
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Space align="center">
          <AuditOutlined style={{ fontSize: 24, color: '#2563eb' }} />
          <div>
            <Title level={3} style={{ margin: 0 }}>
              Enterprise Audit Logs
            </Title>
            <Text type="secondary">
              Immutable compliance trail of all system mutations, user logins, and administrative actions
            </Text>
          </div>
        </Space>
      </div>

      <Card style={{ marginBottom: 24 }}>
        <Space size={16} wrap style={{ width: '100%' }}>
          <Input
            placeholder="Filter by actor email..."
            prefix={<SearchOutlined />}
            value={actorEmail}
            onChange={(e) => setActorEmail(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 220 }}
          />

          <Select
            placeholder="Filter by Action"
            allowClear
            value={actionFilter}
            onChange={(val) => setActionFilter(val)}
            style={{ width: 200 }}
          >
            {ACTION_OPTIONS.map((a) => (
              <Option key={a} value={a}>
                {a}
              </Option>
            ))}
          </Select>

          <Input
            placeholder="Target (e.g. Content)..."
            value={resourceFilter}
            onChange={(e) => setResourceFilter(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 160 }}
          />

          <RangePicker
            value={dateRange}
            onChange={(dates) => setDateRange(dates as any)}
            style={{ width: 260 }}
          />

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
          dataSource={logs}
          rowKey="id"
          loading={loading}
          expandable={{
            expandedRowRender: (record) => (
              <div style={{ margin: 0, padding: '12px 16px', backgroundColor: '#f8fafc', borderRadius: 8 }}>
                <Text strong style={{ display: 'block', marginBottom: 6 }}>
                  Detailed Event Payload:
                </Text>
                <pre
                  style={{
                    backgroundColor: '#0f172a',
                    color: '#e2e8f0',
                    padding: 12,
                    borderRadius: 6,
                    fontSize: 12,
                    overflowX: 'auto',
                    margin: 0,
                  }}
                >
                  {record.metadata || 'No additional payload metadata attached.'}
                </pre>
                <div style={{ marginTop: 8, fontSize: 11, color: '#64748b' }}>
                  Client User-Agent: {record.userAgent || 'Unknown'} | Event UUID: {record.uuid}
                </div>
              </div>
            ),
          }}
          pagination={{
            current: page + 1,
            pageSize,
            total,
            onChange: (p, size) => {
              setPage(p - 1);
              setPageSize(size);
            },
            showSizeChanger: true,
            pageSizeOptions: ['20', '50', '100'],
          }}
        />
      </Card>
    </div>
  );
};
