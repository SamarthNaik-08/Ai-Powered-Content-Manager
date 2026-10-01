import React, { useState, useEffect } from 'react';
import {
  Table, Button, Input, Card, Typography, message, Popconfirm,
  Modal, Form, Space, Row, Col, Tooltip, Tag as AntTag
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, ReloadOutlined, TagsOutlined
} from '@ant-design/icons';
import { contentService } from '@/services/contentService';
import { Tag } from '@/types/content';
import { useAuth } from '@/hooks/useAuth';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export const TagsPage: React.FC = () => {
  const { hasRole } = useAuth();
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => { fetchTags(); }, []);

  const fetchTags = async () => {
    setLoading(true);
    try {
      const data = await contentService.getTags();
      setTags(data);
    } catch (err: any) {
      message.error('Failed to load tags');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    form.resetFields();
    setModalVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      await contentService.createTag(values);
      message.success('Tag created');
      setModalVisible(false);
      fetchTags();
    } catch (err: any) {
      if (err.errorFields) return;
      message.error(err.response?.data?.message || err.message || 'Failed to create tag');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await contentService.deleteTag(id);
      message.success('Tag deleted');
      fetchTags();
    } catch (err: any) {
      message.error(err.response?.data?.message || err.message || 'Failed to delete');
    }
  };

  const columns = [
    {
      title: 'Name', dataIndex: 'name', key: 'name',
      render: (t: string) => <AntTag color="purple" icon={<TagsOutlined />}>{t}</AntTag>
    },
    {
      title: 'Slug', dataIndex: 'slug', key: 'slug',
      render: (t: string) => <Text code style={{ fontSize: 12 }}>{t}</Text>
    },
    {
      title: 'Created', dataIndex: 'createdAt', key: 'createdAt',
      render: (d: string) => dayjs(d).format('MMM DD, YYYY'),
    },
    {
      title: 'Actions', key: 'actions', width: 80,
      render: (_: any, record: Tag) => (
        <Space size="small">
          {hasRole('ROLE_ADMIN') && (
            <Popconfirm title="Delete this tag?" onConfirm={() => handleDelete(record.id)} okButtonProps={{ danger: true }}>
              <Tooltip title="Delete">
                <Button type="text" size="small" danger icon={<DeleteOutlined />} />
              </Tooltip>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={3} style={{ margin: 0 }}>Tags</Title>
          <Text type="secondary">Label and organize content with tags</Text>
        </Col>
        <Col>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchTags} style={{ borderRadius: 8 }} />
            {(hasRole('ROLE_ADMIN') || hasRole('ROLE_EDITOR')) && (
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate} style={{ borderRadius: 8, fontWeight: 600 }}>
                New Tag
              </Button>
            )}
          </Space>
        </Col>
      </Row>

      <Card style={{ borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Table columns={columns} dataSource={tags} rowKey="id" loading={loading}
          pagination={{ pageSize: 20, showTotal: (t) => `${t} tags` }} />
      </Card>

      <Modal
        title="New Tag"
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        okText="Create"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Tag Name" rules={[{ required: true, message: 'Name is required' }, { max: 100 }]}>
            <Input placeholder="Tag name" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
