import React, { useEffect, useState } from 'react';
import {
  Card,
  Row,
  Col,
  Input,
  Button,
  Upload,
  Typography,
  Space,
  Tag,
  Modal,
  Popconfirm,
  message,
  Empty,
  Spin,
  Pagination,
  Tooltip,
  Image,
} from 'antd';
import {
  UploadOutlined,
  SearchOutlined,
  DeleteOutlined,
  CopyOutlined,
  FileOutlined,
  FilePdfOutlined,
  PictureOutlined,
  CheckOutlined,
} from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { mediaService } from '@/services/mediaService';
import { MediaItem } from '@/types/enterprise';

const { Title, Text, Paragraph } = Typography;
const { Search } = Input;
const { Dragger } = Upload;

export const MediaLibraryPage: React.FC = () => {
  const [mediaList, setMediaList] = useState<MediaItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [total, setTotal] = useState<number>(0);
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(12);
  const [search, setSearch] = useState<string>('');
  const [uploadModalOpen, setUploadModalOpen] = useState<boolean>(false);
  const [uploading, setUploading] = useState<boolean>(false);
  const [copiedId, setCopiedId] = useState<number | null>(null);

  const fetchMedia = async (currentPage = page, currentSearch = search) => {
    setLoading(true);
    try {
      const data = await mediaService.getMedia({
        search: currentSearch || undefined,
        page: currentPage,
        size: pageSize,
      });
      setMediaList(data.content);
      setTotal(data.totalElements);
    } catch (err: any) {
      message.error(err.message || 'Failed to load media');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMedia(page, search);
  }, [page, pageSize]);

  const handleSearch = (value: string) => {
    setSearch(value);
    setPage(0);
    fetchMedia(0, value);
  };

  const handleDelete = async (id: number) => {
    try {
      await mediaService.deleteMedia(id);
      message.success('Media item deleted');
      fetchMedia(page, search);
    } catch (err: any) {
      message.error(err.message || 'Failed to delete media');
    }
  };

  const handleCopyUrl = (item: MediaItem) => {
    navigator.clipboard.writeText(window.location.origin + item.storageUrl);
    setCopiedId(item.id);
    message.success('Storage URL copied to clipboard');
    setTimeout(() => setCopiedId(null), 2000);
  };

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    showUploadList: false,
    customRequest: async ({ file, onSuccess, onError }) => {
      setUploading(true);
      try {
        await mediaService.uploadMedia(file as File);
        message.success(`${(file as File).name} uploaded successfully`);
        onSuccess?.('ok');
        setUploadModalOpen(false);
        fetchMedia(0, search);
      } catch (err: any) {
        message.error(err.message || 'Upload failed');
        onError?.(err);
      } finally {
        setUploading(false);
      }
    },
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const renderPreview = (item: MediaItem) => {
    if (item.mimeType.startsWith('image/')) {
      return (
        <div
          style={{
            height: 180,
            overflow: 'hidden',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: '#f8fafc',
          }}
        >
          <Image
            src={item.storageUrl}
            alt={item.originalFilename}
            style={{ maxHeight: 180, width: '100%', objectFit: 'cover' }}
            fallback="data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%23eee' width='100' height='100'/><text fill='%23aaa' x='50%' y='50%' dominant-baseline='middle' text-anchor='middle'>Image</text></svg>"
          />
        </div>
      );
    }
    if (item.mimeType === 'application/pdf') {
      return (
        <div
          style={{
            height: 180,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: '#fef2f2',
          }}
        >
          <FilePdfOutlined style={{ fontSize: 56, color: '#ef4444' }} />
        </div>
      );
    }
    return (
      <div
        style={{
          height: 180,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#f1f5f9',
        }}
      >
        <FileOutlined style={{ fontSize: 56, color: '#64748b' }} />
      </div>
    );
  };

  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 24,
          flexWrap: 'wrap',
          gap: 16,
        }}
      >
        <div>
          <Title level={3} style={{ margin: 0 }}>
            Media Library
          </Title>
          <Text type="secondary">
            Manage images, PDFs, and assets with automated validation and content association
          </Text>
        </div>
        <Button
          type="primary"
          icon={<UploadOutlined />}
          size="large"
          onClick={() => setUploadModalOpen(true)}
        >
          Upload Media
        </Button>
      </div>

      <Card style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={12} md={8}>
            <Search
              placeholder="Search by filename or title..."
              allowClear
              enterButton={<SearchOutlined />}
              size="middle"
              onSearch={handleSearch}
            />
          </Col>
          <Col xs={24} sm={12} md={16} style={{ textAlign: 'right' }}>
            <Text type="secondary">
              Showing <strong>{mediaList.length}</strong> of <strong>{total}</strong> assets
            </Text>
          </Col>
        </Row>
      </Card>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" />
        </div>
      ) : mediaList.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: '40px 0' }}>
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
              <div>
                <Paragraph strong>No media files found</Paragraph>
                <Text type="secondary">
                  Upload pictures, diagrams, and documents to attach them to articles
                </Text>
              </div>
            }
          >
            <Button
              type="primary"
              icon={<UploadOutlined />}
              onClick={() => setUploadModalOpen(true)}
            >
              Upload Asset
            </Button>
          </Empty>
        </Card>
      ) : (
        <>
          <Row gutter={[20, 20]}>
            {mediaList.map((item) => (
              <Col xs={24} sm={12} md={8} lg={6} key={item.id}>
                <Card
                  hoverable
                  cover={renderPreview(item)}
                  style={{
                    borderRadius: 12,
                    overflow: 'hidden',
                    display: 'flex',
                    flexDirection: 'column',
                    height: '100%',
                  }}
                  bodyStyle={{ padding: 14, flex: 1, display: 'flex', flexDirection: 'column' }}
                >
                  <Tooltip title={item.originalFilename}>
                    <Text
                      strong
                      style={{
                        display: 'block',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        marginBottom: 4,
                      }}
                    >
                      {item.originalFilename}
                    </Text>
                  </Tooltip>

                  <Space size={4} wrap style={{ marginBottom: 8 }}>
                    <Tag color="cyan">{formatSize(item.size)}</Tag>
                    <Tag color="geekblue">{item.mimeType.split('/')[1] || item.mimeType}</Tag>
                  </Space>

                  <div style={{ marginTop: 'auto', paddingTop: 8, borderTop: '1px solid #f1f5f9' }}>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                      }}
                    >
                      <Text type="secondary" style={{ fontSize: 11 }}>
                        {new Date(item.createdAt).toLocaleDateString()}
                      </Text>
                      <Space>
                        <Tooltip title="Copy URL">
                          <Button
                            type="text"
                            size="small"
                            icon={copiedId === item.id ? <CheckOutlined style={{ color: '#10b981' }} /> : <CopyOutlined />}
                            onClick={() => handleCopyUrl(item)}
                          />
                        </Tooltip>
                        <Popconfirm
                          title="Delete media file?"
                          description="Are you sure you want to delete this media file permanently?"
                          onConfirm={() => handleDelete(item.id)}
                          okText="Delete"
                          cancelText="Cancel"
                          okButtonProps={{ danger: true }}
                        >
                          <Button
                            type="text"
                            danger
                            size="small"
                            icon={<DeleteOutlined />}
                          />
                        </Popconfirm>
                      </Space>
                    </div>
                  </div>
                </Card>
              </Col>
            ))}
          </Row>

          <div style={{ textAlign: 'right', marginTop: 24 }}>
            <Pagination
              current={page + 1}
              pageSize={pageSize}
              total={total}
              onChange={(p, size) => {
                setPage(p - 1);
                setPageSize(size);
              }}
              showSizeChanger
              pageSizeOptions={['12', '24', '48']}
            />
          </div>
        </>
      )}

      {/* Upload Modal */}
      <Modal
        title="Upload Media Asset"
        open={uploadModalOpen}
        onCancel={() => setUploadModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <div style={{ marginTop: 16 }}>
          <Dragger {...uploadProps} disabled={uploading}>
            <p className="ant-upload-drag-icon">
              <PictureOutlined style={{ fontSize: 48, color: '#3b82f6' }} />
            </p>
            <p className="ant-upload-text">Click or drag file to this area to upload</p>
            <p className="ant-upload-hint">
              Supports JPEG, PNG, WebP, GIF, SVG, and PDF up to 10MB. Automated malware & MIME validation active.
            </p>
          </Dragger>
          {uploading && (
            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <Spin tip="Uploading & validating file..." />
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
};
