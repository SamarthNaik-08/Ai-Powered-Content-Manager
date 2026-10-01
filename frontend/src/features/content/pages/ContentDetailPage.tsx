import React, { useState, useEffect } from 'react';
import {
  Card, Typography, Descriptions, Tag, Space, Button, Divider, message,
  Spin, Popconfirm, Row, Col, Timeline, Modal, DatePicker
} from 'antd';
import {
  ArrowLeftOutlined, EditOutlined, SendOutlined, CheckCircleOutlined,
  CloseCircleOutlined, CloudUploadOutlined, InboxOutlined, HistoryOutlined,
  RollbackOutlined, EyeOutlined, ClockCircleOutlined
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import { contentService } from '@/services/contentService';
import { Content, ContentVersion } from '@/types/content';
import { useAuth } from '@/hooks/useAuth';
import dayjs from 'dayjs';

const { Title, Text, Paragraph } = Typography;

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'default',
  IN_REVIEW: 'processing',
  APPROVED: 'cyan',
  SCHEDULED: 'geekblue',
  PUBLISHED: 'success',
  REJECTED: 'error',
  ARCHIVED: 'warning',
};

export const ContentDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();

  const [content, setContent] = useState<Content | null>(null);
  const [versions, setVersions] = useState<ContentVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [versionModalVisible, setVersionModalVisible] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState<ContentVersion | null>(null);

  const [scheduleModalVisible, setScheduleModalVisible] = useState(false);
  const [scheduledDate, setScheduledDate] = useState<dayjs.Dayjs | null>(null);
  const [scheduling, setScheduling] = useState(false);

  const handleSchedule = async () => {
    if (!scheduledDate || !content) {
      message.error('Please select a date and time in the future');
      return;
    }
    if (scheduledDate.isBefore(dayjs())) {
      message.error('Scheduled date/time must be in the future');
      return;
    }
    setScheduling(true);
    try {
      await contentService.schedule(content.id, scheduledDate.toISOString());
      message.success('Content scheduled for publication successfully');
      setScheduleModalVisible(false);
      loadContent(content.id);
    } catch (err: any) {
      message.error(err.response?.data?.message || err.message || 'Failed to schedule content');
    } finally {
      setScheduling(false);
    }
  };

  useEffect(() => {
    if (id) {
      loadContent(Number(id));
      loadVersions(Number(id));
    }
  }, [id]);

  const loadContent = async (contentId: number) => {
    setLoading(true);
    try {
      const data = await contentService.getContentById(contentId);
      setContent(data);
    } catch (err: any) {
      message.error('Failed to load content');
      navigate('/content');
    } finally {
      setLoading(false);
    }
  };

  const loadVersions = async (contentId: number) => {
    try {
      const data = await contentService.getVersions(contentId, 0, 50);
      setVersions(data.content || []);
    } catch (err: any) {
      // Versions may not exist yet
    }
  };

  const handleWorkflowAction = async (action: string) => {
    if (!content?.id) return;
    try {
      switch (action) {
        case 'submit': await contentService.submitForReview(content.id); break;
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

  const handleRestoreVersion = async (versionNumber: number) => {
    if (!content?.id) return;
    try {
      await contentService.restoreVersion(content.id, versionNumber);
      message.success(`Version ${versionNumber} restored`);
      loadContent(content.id);
      loadVersions(content.id);
    } catch (err: any) {
      message.error(err.response?.data?.message || err.message || 'Failed to restore version');
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <Spin size="large" tip="Loading..." />
      </div>
    );
  }

  if (!content) return null;

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/content')} style={{ borderRadius: 8 }}>Back</Button>
            <div>
              <Title level={3} style={{ margin: 0 }}>{content.title}</Title>
              <Space style={{ marginTop: 4 }}>
                <Tag color={STATUS_COLORS[content.status]}>{content.status.replace('_', ' ')}</Tag>
                <Text type="secondary" style={{ fontSize: 12 }}>by {content.author?.name}</Text>
                <Text type="secondary" style={{ fontSize: 12 }}>• {dayjs(content.createdAt).format('MMM DD, YYYY HH:mm')}</Text>
              </Space>
            </div>
          </Space>
        </Col>
        <Col>
          <Space>
            {hasPermission('CONTENT_UPDATE') && (
              <Button icon={<EditOutlined />} onClick={() => navigate(`/content/${content.id}/edit`)} style={{ borderRadius: 8 }}>
                Edit
              </Button>
            )}
            {content.status === 'DRAFT' && hasPermission('CONTENT_SUBMIT') && (
              <Button type="primary" icon={<SendOutlined />} onClick={() => handleWorkflowAction('submit')}
                style={{ borderRadius: 8, background: '#7c3aed' }}>Submit</Button>
            )}
            {content.status === 'IN_REVIEW' && hasPermission('CONTENT_APPROVE') && (
              <>
                <Button type="primary" icon={<CheckCircleOutlined />} onClick={() => handleWorkflowAction('approve')}
                  style={{ borderRadius: 8, background: '#10b981' }}>Approve</Button>
                <Button danger icon={<CloseCircleOutlined />} onClick={() => handleWorkflowAction('reject')}
                  style={{ borderRadius: 8 }}>Reject</Button>
              </>
            )}
            {(content.status === 'APPROVED' || content.status === 'SCHEDULED') && hasPermission('CONTENT_PUBLISH') && (
              <Button type="primary" icon={<CloudUploadOutlined />} onClick={() => handleWorkflowAction('publish')}
                style={{ borderRadius: 8 }}>Publish</Button>
            )}
            {content.status === 'APPROVED' && hasPermission('CONTENT_PUBLISH') && (
              <Button icon={<ClockCircleOutlined />} onClick={() => setScheduleModalVisible(true)}
                style={{ borderRadius: 8, borderColor: '#0891b2', color: '#0891b2' }}>Schedule</Button>
            )}
            {content.status !== 'ARCHIVED' && hasPermission('CONTENT_UPDATE') && (
              <Button icon={<InboxOutlined />} onClick={() => handleWorkflowAction('archive')} style={{ borderRadius: 8 }}>Archive</Button>
            )}
          </Space>
        </Col>
      </Row>

      <Row gutter={24}>
        <Col xs={24} lg={16}>
          <Card title="Content Body" style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.8, fontSize: 15 }}>
              {content.body || <Text type="secondary" italic>No content body yet.</Text>}
            </div>
          </Card>

          {content.excerpt && (
            <Card title="Excerpt" style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              <Paragraph>{content.excerpt}</Paragraph>
            </Card>
          )}

          <Card
            title={<Space><HistoryOutlined /> Version History</Space>}
            style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}
          >
            {versions.length === 0 ? (
              <Text type="secondary">No versions recorded yet.</Text>
            ) : (
              <Timeline
                items={versions.map(v => ({
                  color: 'blue',
                  children: (
                    <div>
                      <Space>
                        <Text strong>v{v.versionNumber}</Text>
                        <Text type="secondary">{v.changeSummary}</Text>
                        <Text type="secondary" style={{ fontSize: 12 }}>{dayjs(v.createdAt).format('MMM DD, YYYY HH:mm')}</Text>
                        <Text type="secondary" style={{ fontSize: 12 }}>by {v.createdByName}</Text>
                      </Space>
                      <div style={{ marginTop: 4 }}>
                        <Space size="small">
                          <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => { setSelectedVersion(v); setVersionModalVisible(true); }}>
                            View
                          </Button>
                          {hasPermission('CONTENT_UPDATE') && (
                            <Popconfirm title={`Restore to version ${v.versionNumber}?`} onConfirm={() => handleRestoreVersion(v.versionNumber)}>
                              <Button size="small" type="link" icon={<RollbackOutlined />}>Restore</Button>
                            </Popconfirm>
                          )}
                        </Space>
                      </div>
                    </div>
                  ),
                }))}
              />
            )}
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card title="Details" style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Content Type">
                <Tag>{content.contentType.replace('_', ' ')}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Category">
                {content.category?.name || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Tags">
                <Space wrap>
                  {content.tags?.length > 0 ? content.tags.map(t => (
                    <Tag key={t.id} color="purple">{t.name}</Tag>
                  )) : '—'}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Author">{content.author?.name}</Descriptions.Item>
              <Descriptions.Item label="Published">
                {content.publishedAt ? dayjs(content.publishedAt).format('MMM DD, YYYY HH:mm') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Created">{dayjs(content.createdAt).format('MMM DD, YYYY HH:mm')}</Descriptions.Item>
              <Descriptions.Item label="Updated">{dayjs(content.updatedAt).format('MMM DD, YYYY HH:mm')}</Descriptions.Item>
              <Descriptions.Item label="Slug">
                <Text code style={{ fontSize: 12 }}>{content.slug}</Text>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {(content.seoTitle || content.seoDescription) && (
            <Card title="SEO" style={{ borderRadius: 12, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
              {content.seoTitle && (
                <div style={{ marginBottom: 8 }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>SEO Title</Text>
                  <div>{content.seoTitle}</div>
                </div>
              )}
              {content.seoDescription && (
                <div>
                  <Text type="secondary" style={{ fontSize: 12 }}>SEO Description</Text>
                  <div>{content.seoDescription}</div>
                </div>
              )}
            </Card>
          )}
        </Col>
      </Row>

      <Modal
        title={selectedVersion ? `Version ${selectedVersion.versionNumber}` : 'Version'}
        open={versionModalVisible}
        onCancel={() => setVersionModalVisible(false)}
        footer={null}
        width={800}
      >
        {selectedVersion && (
          <div>
            <Descriptions column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Title">{selectedVersion.title}</Descriptions.Item>
              <Descriptions.Item label="Change Summary">{selectedVersion.changeSummary}</Descriptions.Item>
              <Descriptions.Item label="Created By">{selectedVersion.createdByName}</Descriptions.Item>
              <Descriptions.Item label="Created At">{dayjs(selectedVersion.createdAt).format('MMM DD, YYYY HH:mm')}</Descriptions.Item>
            </Descriptions>
            <Divider />
            <Title level={5}>Body</Title>
            <div style={{ whiteSpace: 'pre-wrap', background: '#f8f9fa', padding: 16, borderRadius: 8, maxHeight: 400, overflow: 'auto' }}>
              {selectedVersion.body || <Text type="secondary" italic>Empty body</Text>}
            </div>
          </div>
        )}
      </Modal>

      {/* Schedule Publication Modal */}
      <Modal
        title="Schedule Publication"
        open={scheduleModalVisible}
        onOk={handleSchedule}
        confirmLoading={scheduling}
        onCancel={() => setScheduleModalVisible(false)}
        okText="Confirm Schedule"
        destroyOnClose
      >
        <div style={{ padding: '16px 0' }}>
          <Paragraph type="secondary">
            Select a target date and time for automated background publishing. The scheduler runs periodically and atomically transitions the content to Published with notifications and audit tracking.
          </Paragraph>
          <div style={{ marginTop: 16 }}>
            <Text strong style={{ display: 'block', marginBottom: 8 }}>
              Publication Date & Time:
            </Text>
            <DatePicker
              showTime
              minDate={dayjs().add(1, 'minute')}
              value={scheduledDate}
              onChange={(d) => setScheduledDate(d)}
              style={{ width: '100%' }}
              placeholder="Select future date & time"
            />
          </div>
        </div>
      </Modal>
    </div>
  );
};
