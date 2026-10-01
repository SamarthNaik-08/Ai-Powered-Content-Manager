import React, { useState, useEffect } from 'react';
import {
  Table, Button, Input, Select, Tag, Space, Card, Typography,
  message, Popconfirm, Row, Col, Badge, Tooltip, Dropdown
} from 'antd';
import {
  PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined,
  EyeOutlined, SendOutlined, CheckCircleOutlined, CloseCircleOutlined,
  CloudUploadOutlined, InboxOutlined, ReloadOutlined
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { MenuProps } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { contentService, ContentSearchParams } from '@/services/contentService';
import { Content, ContentStatus, ContentType, PageResponse } from '@/types/content';
import { useAuth } from '@/hooks/useAuth';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { Option } = Select;

const STATUS_COLORS: Record<ContentStatus, string> = {
  DRAFT: 'default',
  IN_REVIEW: 'processing',
  APPROVED: 'cyan',
  SCHEDULED: 'geekblue',
  PUBLISHED: 'success',
  REJECTED: 'error',
  ARCHIVED: 'warning',
};

const CONTENT_TYPE_LABELS: Record<ContentType, string> = {
  BLOG_POST: 'Blog Post',
  ARTICLE: 'Article',
  NEWS: 'News',
  SOCIAL_POST: 'Social Post',
  DOCUMENT: 'Document',
  ANNOUNCEMENT: 'Announcement',
};

export const ContentListPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { hasPermission } = useAuth();

  const [data, setData] = useState<PageResponse<Content> | null>(null);
  const [loading, setLoading] = useState(false);

  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [statusFilter, setStatusFilter] = useState<ContentStatus | undefined>(
    (searchParams.get('status') as ContentStatus) || undefined
  );
  const [typeFilter, setTypeFilter] = useState<ContentType | undefined>(
    (searchParams.get('contentType') as ContentType) || undefined
  );
  const [page, setPage] = useState(Number(searchParams.get('page')) || 0);
  const [pageSize, setPageSize] = useState(Number(searchParams.get('size')) || 20);

  const fetchContents = async () => {
    setLoading(true);
    try {
      const params: ContentSearchParams = {
        page,
        size: pageSize,
        sortBy: 'createdAt',
        sortDir: 'desc',
      };
      if (keyword) params.keyword = keyword;
      if (statusFilter) params.status = statusFilter;
      if (typeFilter) params.contentType = typeFilter;

      const result = await contentService.searchContents(params);
      setData(result);
    } catch (err: any) {
      message.error(err.message || 'Failed to fetch contents');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchContents();
  }, [page, pageSize, statusFilter, typeFilter]);

  const handleSearch = () => {
    setPage(0);
    fetchContents();
  };

  const handleWorkflowAction = async (id: number, action: string) => {
    try {
      switch (action) {
        case 'submit': await contentService.submitForReview(id); break;
        case 'approve': await contentService.approve(id); break;
        case 'reject': await contentService.reject(id); break;
        case 'publish': await contentService.publish(id); break;
        case 'archive': await contentService.archive(id); break;
      }
      message.success(`Content ${action}ed successfully`);
      fetchContents();
    } catch (err: any) {
      message.error(err.response?.data?.message || err.message || `Failed to ${action} content`);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await contentService.deleteContent(id);
      message.success('Content deleted successfully');
      fetchContents();
    } catch (err: any) {
      message.error(err.message || 'Failed to delete content');
    }
  };

  const getWorkflowActions = (record: Content): MenuProps['items'] => {
    const items: MenuProps['items'] = [];
    if (record.status === 'DRAFT' && hasPermission('CONTENT_SUBMIT')) {
      items.push({ key: 'submit', icon: <SendOutlined />, label: 'Submit for Review', onClick: () => handleWorkflowAction(record.id, 'submit') });
    }
    if (record.status === 'IN_REVIEW' && hasPermission('CONTENT_APPROVE')) {
      items.push({ key: 'approve', icon: <CheckCircleOutlined />, label: 'Approve', onClick: () => handleWorkflowAction(record.id, 'approve') });
      items.push({ key: 'reject', icon: <CloseCircleOutlined />, label: 'Reject', danger: true, onClick: () => handleWorkflowAction(record.id, 'reject') });
    }
    if ((record.status === 'APPROVED' || record.status === 'SCHEDULED') && hasPermission('CONTENT_PUBLISH')) {
      items.push({ key: 'publish', icon: <CloudUploadOutlined />, label: 'Publish', onClick: () => handleWorkflowAction(record.id, 'publish') });
    }
    if (record.status !== 'ARCHIVED' && hasPermission('CONTENT_UPDATE')) {
      items.push({ key: 'archive', icon: <InboxOutlined />, label: 'Archive', onClick: () => handleWorkflowAction(record.id, 'archive') });
    }
    return items;
  };

  const columns: ColumnsType<Content> = [
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
      width: 280,
      render: (text: string, record: Content) => (
        <Space direction="vertical" size={0}>
          <Text strong style={{ cursor: 'pointer', color: '#2563eb' }} onClick={() => navigate(`/content/${record.id}`)}>
            {text}
          </Text>
          <Text type="secondary" style={{ fontSize: 12 }}>{record.slug}</Text>
        </Space>
      ),
    },
    {
      title: 'Type',
      dataIndex: 'contentType',
      key: 'contentType',
      width: 120,
      render: (type: ContentType) => (
        <Tag style={{ borderRadius: 4 }}>{CONTENT_TYPE_LABELS[type]}</Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: ContentStatus) => (
        <Badge status={status === 'PUBLISHED' ? 'success' : status === 'REJECTED' ? 'error' : 'processing'} />
      ),
      filters: Object.keys(STATUS_COLORS).map(s => ({ text: s.replace('_', ' '), value: s })),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'statusTag',
      width: 120,
      render: (status: ContentStatus) => (
        <Tag color={STATUS_COLORS[status]}>{status.replace('_', ' ')}</Tag>
      ),
    },
    {
      title: 'Author',
      key: 'author',
      width: 140,
      render: (_: any, record: Content) => record.author?.name || 'Unknown',
    },
    {
      title: 'Category',
      key: 'category',
      width: 120,
      render: (_: any, record: Content) => record.category?.name || '—',
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 130,
      render: (date: string) => dayjs(date).format('MMM DD, YYYY'),
      sorter: true,
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 200,
      fixed: 'right',
      render: (_: any, record: Content) => {
        const workflowItems = getWorkflowActions(record);
        return (
          <Space size="small">
            <Tooltip title="View">
              <Button type="text" size="small" icon={<EyeOutlined />} onClick={() => navigate(`/content/${record.id}`)} />
            </Tooltip>
            {hasPermission('CONTENT_UPDATE') && (
              <Tooltip title="Edit">
                <Button type="text" size="small" icon={<EditOutlined />} onClick={() => navigate(`/content/${record.id}/edit`)} />
              </Tooltip>
            )}
            {workflowItems && workflowItems.length > 0 && (
              <Dropdown menu={{ items: workflowItems }} placement="bottomRight">
                <Button type="text" size="small" icon={<SendOutlined />} />
              </Dropdown>
            )}
            {hasPermission('CONTENT_DELETE') && (
              <Popconfirm title="Delete this content?" onConfirm={() => handleDelete(record.id)} okText="Delete" cancelText="Cancel" okButtonProps={{ danger: true }}>
                <Tooltip title="Delete">
                  <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                </Tooltip>
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  // Remove duplicate status column
  const visibleColumns = columns.filter((col, idx) => !(col.key === 'status' && idx === 2));

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={3} style={{ margin: 0 }}>Content Management</Title>
          <Text type="secondary">Manage your content, workflow, and publishing pipeline</Text>
        </Col>
        <Col>
          {hasPermission('CONTENT_CREATE') && (
            <Button type="primary" icon={<PlusOutlined />} size="large" onClick={() => navigate('/content/create')} style={{ borderRadius: 8, fontWeight: 600 }}>
              New Content
            </Button>
          )}
        </Col>
      </Row>

      <Card style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Row gutter={[12, 12]} align="middle">
          <Col flex="auto">
            <Input
              placeholder="Search content by title, description, or body..."
              prefix={<SearchOutlined />}
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
              onPressEnter={handleSearch}
              allowClear
              style={{ borderRadius: 8 }}
            />
          </Col>
          <Col>
            <Select
              placeholder="Status"
              allowClear
              value={statusFilter}
              onChange={v => { setStatusFilter(v); setPage(0); }}
              style={{ width: 140, borderRadius: 8 }}
            >
              {Object.keys(STATUS_COLORS).map(s => (
                <Option key={s} value={s}>{s.replace('_', ' ')}</Option>
              ))}
            </Select>
          </Col>
          <Col>
            <Select
              placeholder="Type"
              allowClear
              value={typeFilter}
              onChange={v => { setTypeFilter(v); setPage(0); }}
              style={{ width: 140, borderRadius: 8 }}
            >
              {Object.entries(CONTENT_TYPE_LABELS).map(([k, v]) => (
                <Option key={k} value={k}>{v}</Option>
              ))}
            </Select>
          </Col>
          <Col>
            <Button icon={<SearchOutlined />} type="primary" onClick={handleSearch} style={{ borderRadius: 8 }}>
              Search
            </Button>
          </Col>
          <Col>
            <Tooltip title="Refresh">
              <Button icon={<ReloadOutlined />} onClick={fetchContents} style={{ borderRadius: 8 }} />
            </Tooltip>
          </Col>
        </Row>
      </Card>

      <Card style={{ borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Table<Content>
          columns={visibleColumns}
          dataSource={data?.content || []}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1200 }}
          pagination={{
            current: (data?.number || 0) + 1,
            pageSize: data?.size || pageSize,
            total: data?.totalElements || 0,
            showSizeChanger: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} items`,
            onChange: (p, s) => {
              setPage(p - 1);
              setPageSize(s);
            },
          }}
        />
      </Card>
    </div>
  );
};
