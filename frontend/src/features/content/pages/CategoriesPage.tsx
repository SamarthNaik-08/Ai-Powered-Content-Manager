import React, { useState, useEffect } from 'react';
import {
  Table, Button, Input, Card, Typography, message, Popconfirm,
  Modal, Form, Space, Row, Col, Tooltip
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined
} from '@ant-design/icons';
import { contentService } from '@/services/contentService';
import { Category } from '@/types/content';
import { useAuth } from '@/hooks/useAuth';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export const CategoriesPage: React.FC = () => {
  const { hasRole } = useAuth();
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [form] = Form.useForm();

  useEffect(() => { fetchCategories(); }, []);

  const fetchCategories = async () => {
    setLoading(true);
    try {
      const data = await contentService.getCategories();
      setCategories(data);
    } catch (err: any) {
      message.error('Failed to load categories');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingCategory(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (category: Category) => {
    setEditingCategory(category);
    form.setFieldsValue({ name: category.name, description: category.description });
    setModalVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingCategory) {
        await contentService.updateCategory(editingCategory.id, values);
        message.success('Category updated');
      } else {
        await contentService.createCategory(values);
        message.success('Category created');
      }
      setModalVisible(false);
      fetchCategories();
    } catch (err: any) {
      if (err.errorFields) return;
      message.error(err.response?.data?.message || err.message || 'Failed to save category');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await contentService.deleteCategory(id);
      message.success('Category deleted');
      fetchCategories();
    } catch (err: any) {
      message.error(err.response?.data?.message || err.message || 'Failed to delete');
    }
  };

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name', render: (t: string) => <Text strong>{t}</Text> },
    { title: 'Slug', dataIndex: 'slug', key: 'slug', render: (t: string) => <Text code style={{ fontSize: 12 }}>{t}</Text> },
    { title: 'Description', dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: 'Created', dataIndex: 'createdAt', key: 'createdAt',
      render: (d: string) => dayjs(d).format('MMM DD, YYYY'),
    },
    {
      title: 'Actions', key: 'actions', width: 120,
      render: (_: any, record: Category) => (
        <Space size="small">
          {(hasRole('ROLE_ADMIN') || hasRole('ROLE_EDITOR')) && (
            <Tooltip title="Edit">
              <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
            </Tooltip>
          )}
          {hasRole('ROLE_ADMIN') && (
            <Popconfirm title="Delete this category?" onConfirm={() => handleDelete(record.id)} okButtonProps={{ danger: true }}>
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
          <Title level={3} style={{ margin: 0 }}>Categories</Title>
          <Text type="secondary">Organize content into categories</Text>
        </Col>
        <Col>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchCategories} style={{ borderRadius: 8 }} />
            {(hasRole('ROLE_ADMIN') || hasRole('ROLE_EDITOR')) && (
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate} style={{ borderRadius: 8, fontWeight: 600 }}>
                New Category
              </Button>
            )}
          </Space>
        </Col>
      </Row>

      <Card style={{ borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <Table columns={columns} dataSource={categories} rowKey="id" loading={loading}
          pagination={{ pageSize: 20, showTotal: (t) => `${t} categories` }} />
      </Card>

      <Modal
        title={editingCategory ? 'Edit Category' : 'New Category'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        okText={editingCategory ? 'Update' : 'Create'}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }, { max: 100 }]}>
            <Input placeholder="Category name" />
          </Form.Item>
          <Form.Item name="description" label="Description" rules={[{ max: 500 }]}>
            <Input.TextArea rows={3} placeholder="Description (optional)" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
