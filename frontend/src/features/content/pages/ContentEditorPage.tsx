import React, { useState, useEffect } from 'react';
import {
  Form, Input, Select, Button, Card, Row, Col, Typography, message,
  Space, Spin, Tag as AntTag
} from 'antd';
import {
  SaveOutlined, SendOutlined, ArrowLeftOutlined, RobotOutlined
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { contentService } from '@/services/contentService';
import { Content, ContentType, Category, Tag, CreateContentPayload, UpdateContentPayload } from '@/types/content';
import { useAuth } from '@/hooks/useAuth';
import { AIAssistantPanel } from '../components/AIAssistantPanel';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

const CONTENT_TYPES: { value: ContentType; label: string }[] = [
  { value: 'BLOG_POST', label: 'Blog Post' },
  { value: 'ARTICLE', label: 'Article' },
  { value: 'NEWS', label: 'News' },
  { value: 'SOCIAL_POST', label: 'Social Post' },
  { value: 'DOCUMENT', label: 'Document' },
  { value: 'ANNOUNCEMENT', label: 'Announcement' },
];

interface ContentEditorPageProps {
  mode: 'create' | 'edit';
}

export const ContentEditorPage: React.FC<ContentEditorPageProps> = ({ mode }) => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const [form] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [content, setContent] = useState<Content | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [aiPanelVisible, setAiPanelVisible] = useState(false);

  const handleApplyGenerated = (data: { title?: string; body?: string; excerpt?: string; tagIds?: number[] }) => {
    if (data.title) form.setFieldValue('title', data.title);
    if (data.body) form.setFieldValue('body', data.body);
    if (data.excerpt) form.setFieldValue('excerpt', data.excerpt);
    if (data.tagIds && data.tagIds.length > 0) {
      const currentTags = form.getFieldValue('tagIds') || [];
      const merged = Array.from(new Set([...currentTags, ...data.tagIds]));
      form.setFieldValue('tagIds', merged);
    }
  };

  const handleInsertContent = (text: string) => {
    const current = form.getFieldValue('body') || '';
    form.setFieldValue('body', current ? `${current}\n\n${text}` : text);
  };

  const handleReplaceBody = (text: string) => {
    form.setFieldValue('body', text);
  };

  const handleApplyTitle = (newTitle: string) => {
    form.setFieldValue('title', newTitle);
  };

  const handleApplySeo = (seoData: { seoTitle?: string; seoDescription?: string; slug?: string }) => {
    if (seoData.seoTitle) form.setFieldValue('seoTitle', seoData.seoTitle);
    if (seoData.seoDescription) form.setFieldValue('seoDescription', seoData.seoDescription);
  };

  useEffect(() => {
    loadCategoriesAndTags();
    if (mode === 'edit' && id) {
      loadContent(Number(id));
    }
  }, [mode, id]);

  const loadCategoriesAndTags = async () => {
    try {
      const [cats, tgs] = await Promise.all([
        contentService.getCategories(),
        contentService.getTags(),
      ]);
      setCategories(cats);
      setTags(tgs);
    } catch (err: any) {
      message.error('Failed to load categories/tags');
    }
  };

  const loadContent = async (contentId: number) => {
    setLoading(true);
    try {
      const data = await contentService.getContentById(contentId);
      setContent(data);
      form.setFieldsValue({
        title: data.title,
        contentType: data.contentType,
        categoryId: data.category?.id || null,
        tagIds: data.tags?.map(t => t.id) || [],
        body: data.body || '',
        excerpt: data.excerpt || '',
        description: data.description || '',
        seoTitle: data.seoTitle || '',
        seoDescription: data.seoDescription || '',
        featuredImage: data.featuredImage || '',
      });
    } catch (err: any) {
      message.error('Failed to load content');
      navigate('/content');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveDraft = async () => {
    try {
      await form.validateFields(['title', 'contentType']);
      const allValues = form.getFieldsValue();
      setSaving(true);

      if (mode === 'create') {
        const payload: CreateContentPayload = {
          title: allValues.title,
          contentType: allValues.contentType,
          description: allValues.description,
          body: allValues.body,
          excerpt: allValues.excerpt,
          categoryId: allValues.categoryId || null,
          tagIds: allValues.tagIds || [],
          seoTitle: allValues.seoTitle,
          seoDescription: allValues.seoDescription,
          featuredImage: allValues.featuredImage,
        };
        const created = await contentService.createContent(payload);
        message.success('Content saved as draft');
        navigate(`/content/${created.id}/edit`);
      } else if (id) {
        const payload: UpdateContentPayload = {
          title: allValues.title,
          contentType: allValues.contentType,
          description: allValues.description,
          body: allValues.body,
          excerpt: allValues.excerpt,
          categoryId: allValues.categoryId || null,
          tagIds: allValues.tagIds || [],
          seoTitle: allValues.seoTitle,
          seoDescription: allValues.seoDescription,
          featuredImage: allValues.featuredImage,
          changeSummary: 'Manual save',
        };
        await contentService.updateContent(Number(id), payload);
        message.success('Content saved');
        loadContent(Number(id));
      }
    } catch (err: any) {
      if (err.errorFields) return;
      message.error(err.response?.data?.message || err.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const handleSubmitReview = async () => {
    if (!content?.id) {
      message.warning('Please save the content first');
      return;
    }
    setSubmitting(true);
    try {
      await contentService.submitForReview(content.id);
      message.success('Content submitted for review');
      loadContent(content.id);
    } catch (err: any) {
      message.error(err.response?.data?.message || err.message || 'Failed to submit');
    } finally {
      setSubmitting(false);
    }
  };

  const handleWorkflowAction = async (action: string) => {
    if (!content?.id) return;
    try {
      switch (action) {
        case 'approve': await contentService.approve(content.id); break;
        case 'reject': await contentService.reject(content.id); break;
        case 'publish': await contentService.publish(content.id); break;
        case 'archive': await contentService.archive(content.id); break;
      }
      message.success(`Content ${action}d successfully`);
      loadContent(content.id);
    } catch (err: any) {
      message.error(err.response?.data?.message || err.message || `Failed to ${action}`);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <Spin size="large" tip="Loading content..." />
      </div>
    );
  }

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/content')} style={{ borderRadius: 8 }}>
              Back
            </Button>
            <div>
              <Title level={3} style={{ margin: 0 }}>
                {mode === 'create' ? 'Create New Content' : `Edit: ${content?.title || ''}`}
              </Title>
              {content && (
                <Space size={8} style={{ marginTop: 4 }}>
                  <AntTag color={content.status === 'PUBLISHED' ? 'success' : content.status === 'DRAFT' ? 'default' : 'processing'}>
                    {content.status.replace('_', ' ')}
                  </AntTag>
                  <Text type="secondary" style={{ fontSize: 12 }}>ID: {content.id}</Text>
                </Space>
              )}
            </div>
          </Space>
        </Col>
        <Col>
          <Space>
            {hasPermission('AI_GENERATE') && (
              <Button
                type="primary"
                icon={<RobotOutlined />}
                onClick={() => setAiPanelVisible(true)}
                style={{
                  borderRadius: 8,
                  fontWeight: 600,
                  background: 'linear-gradient(135deg, #1677ff 0%, #722ed1 100%)',
                  border: 'none',
                  boxShadow: '0 2px 6px rgba(114, 46, 209, 0.35)',
                }}
              >
                AI Assistant
              </Button>
            )}

            <Button icon={<SaveOutlined />} onClick={handleSaveDraft} loading={saving}
              style={{ borderRadius: 8, fontWeight: 600 }}>
              {mode === 'create' ? 'Save Draft' : 'Save'}
            </Button>

            {content && content.status === 'DRAFT' && hasPermission('CONTENT_SUBMIT') && (
              <Button type="primary" icon={<SendOutlined />} onClick={handleSubmitReview} loading={submitting}
                style={{ borderRadius: 8, fontWeight: 600, background: '#7c3aed' }}>
                Submit for Review
              </Button>
            )}

            {content && content.status === 'IN_REVIEW' && hasPermission('CONTENT_APPROVE') && (
              <>
                <Button type="primary" onClick={() => handleWorkflowAction('approve')}
                  style={{ borderRadius: 8, fontWeight: 600, background: '#10b981' }}>
                  Approve
                </Button>
                <Button danger onClick={() => handleWorkflowAction('reject')} style={{ borderRadius: 8, fontWeight: 600 }}>
                  Reject
                </Button>
              </>
            )}

            {content && (content.status === 'APPROVED' || content.status === 'SCHEDULED') && hasPermission('CONTENT_PUBLISH') && (
              <Button type="primary" onClick={() => handleWorkflowAction('publish')}
                style={{ borderRadius: 8, fontWeight: 600, background: '#2563eb' }}>
                Publish
              </Button>
            )}

            {content && content.status !== 'ARCHIVED' && hasPermission('CONTENT_UPDATE') && (
              <Button onClick={() => handleWorkflowAction('archive')}
                style={{ borderRadius: 8 }}>
                Archive
              </Button>
            )}
          </Space>
        </Col>
      </Row>

      <Form form={form} layout="vertical" size="large">
        <Row gutter={24}>
          <Col xs={24} lg={16}>
            <Card title="Content" style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Form.Item name="title" label="Title" rules={[{ required: true, message: 'Title is required' }, { max: 300 }]}>
                <Input placeholder="Enter content title" style={{ borderRadius: 8 }} />
              </Form.Item>

              <Form.Item name="body" label="Body">
                <TextArea rows={16} placeholder="Write your content here..." style={{ borderRadius: 8, fontFamily: "'Inter', monospace", fontSize: 14 }} />
              </Form.Item>

              <Form.Item name="excerpt" label="Excerpt">
                <TextArea rows={3} maxLength={500} showCount placeholder="Brief summary..." style={{ borderRadius: 8 }} />
              </Form.Item>

              <Form.Item name="description" label="Description">
                <TextArea rows={2} maxLength={1000} showCount placeholder="Content description..." style={{ borderRadius: 8 }} />
              </Form.Item>
            </Card>
          </Col>

          <Col xs={24} lg={8}>
            <Card title="Properties" style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Form.Item name="contentType" label="Content Type" rules={[{ required: true, message: 'Content type is required' }]}>
                <Select placeholder="Select type" style={{ borderRadius: 8 }}>
                  {CONTENT_TYPES.map(ct => (
                    <Option key={ct.value} value={ct.value}>{ct.label}</Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item name="categoryId" label="Category">
                <Select placeholder="Select category" allowClear style={{ borderRadius: 8 }}>
                  {categories.map(cat => (
                    <Option key={cat.id} value={cat.id}>{cat.name}</Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item name="tagIds" label="Tags">
                <Select mode="multiple" placeholder="Select tags" allowClear style={{ borderRadius: 8 }}
                  optionFilterProp="children">
                  {tags.map(tag => (
                    <Option key={tag.id} value={tag.id}>{tag.name}</Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item name="featuredImage" label="Featured Image URL">
                <Input placeholder="https://..." style={{ borderRadius: 8 }} />
              </Form.Item>
            </Card>

            <Card title="SEO Settings" style={{ borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Form.Item name="seoTitle" label="SEO Title">
                <Input maxLength={300} showCount placeholder="SEO title..." style={{ borderRadius: 8 }} />
              </Form.Item>

              <Form.Item name="seoDescription" label="SEO Description">
                <TextArea rows={3} maxLength={500} showCount placeholder="SEO description..." style={{ borderRadius: 8 }} />
              </Form.Item>
            </Card>
          </Col>
        </Row>
      </Form>

      <AIAssistantPanel
        visible={aiPanelVisible}
        onClose={() => setAiPanelVisible(false)}
        currentTitle={form.getFieldValue('title') || ''}
        currentBody={form.getFieldValue('body') || ''}
        currentExcerpt={form.getFieldValue('excerpt') || ''}
        currentContentType={form.getFieldValue('contentType') || 'ARTICLE'}
        availableTags={tags}
        contentId={content?.id}
        onApplyGenerated={handleApplyGenerated}
        onInsertContent={handleInsertContent}
        onReplaceBody={handleReplaceBody}
        onApplyTitle={handleApplyTitle}
        onApplySeo={handleApplySeo}
      />
    </div>
  );
};
