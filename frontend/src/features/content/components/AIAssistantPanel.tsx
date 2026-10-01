import React, { useState, useEffect } from 'react';
import {
  Drawer, Tabs, Form, Input, Select, Button, Space, Typography, Card,
  Alert, Tag, Divider, Progress, Row, Col, Tooltip, message
} from 'antd';
import {
  RobotOutlined, ThunderboltOutlined, CopyOutlined, CheckOutlined,
  ReloadOutlined, CloseOutlined, FormOutlined, FileTextOutlined,
  CompassOutlined, SearchOutlined, BarChartOutlined, PlusOutlined,
  SwapOutlined
} from '@ant-design/icons';
import { aiService } from '@/services/aiService';
import {
  GenerateContentResponse, RewriteContentResponse, SummarizeContentResponse,
  GenerateTitlesResponse, GenerateSeoResponse, AnalyzeContentResponse,
  AIUsageResponse
} from '@/types/ai';
import { Tag as CmsTag } from '@/types/content';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;
const { Option } = Select;

interface AIAssistantPanelProps {
  visible: boolean;
  onClose: () => void;
  currentTitle: string;
  currentBody: string;
  currentExcerpt: string;
  currentContentType: string;
  availableTags: CmsTag[];
  contentId?: number;
  onApplyGenerated: (data: { title?: string; body?: string; excerpt?: string; tagIds?: number[] }) => void;
  onInsertContent: (text: string) => void;
  onReplaceBody: (text: string) => void;
  onApplyTitle: (title: string) => void;
  onApplySeo: (data: { seoTitle?: string; seoDescription?: string; slug?: string }) => void;
}

export const AIAssistantPanel: React.FC<AIAssistantPanelProps> = ({
  visible,
  onClose,
  currentTitle,
  currentBody,
  currentExcerpt,
  currentContentType,
  availableTags,
  contentId,
  onApplyGenerated,
  onInsertContent,
  onReplaceBody,
  onApplyTitle,
  onApplySeo,
}) => {
  const [activeTab, setActiveTab] = useState('generate');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [usage, setUsage] = useState<AIUsageResponse | null>(null);

  // Operation Results
  const [genResult, setGenResult] = useState<GenerateContentResponse | null>(null);
  const [rewriteResult, setRewriteResult] = useState<RewriteContentResponse | null>(null);
  const [summarizeResult, setSummarizeResult] = useState<SummarizeContentResponse | null>(null);
  const [titlesResult, setTitlesResult] = useState<GenerateTitlesResponse | null>(null);
  const [seoResult, setSeoResult] = useState<GenerateSeoResponse | null>(null);
  const [analyzeResult, setAnalyzeResult] = useState<AnalyzeContentResponse | null>(null);

  // Forms
  const [genForm] = Form.useForm();
  const [rewriteForm] = Form.useForm();

  useEffect(() => {
    if (visible) {
      loadUsage();
      if (currentTitle && !genForm.getFieldValue('topic')) {
        genForm.setFieldsValue({ topic: currentTitle, contentType: currentContentType });
      }
      if (currentBody && !rewriteForm.getFieldValue('content')) {
        rewriteForm.setFieldsValue({ content: currentBody });
      }
    }
  }, [visible, currentTitle, currentBody, currentContentType]);

  const loadUsage = async () => {
    try {
      const data = await aiService.getUsage();
      setUsage(data);
    } catch {
      // Ignore background usage loading error
    }
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    message.success('Copied to clipboard');
  };

  // 1. Generate Handler
  const handleGenerate = async (values: any) => {
    setLoading(true);
    setError(null);
    try {
      const res = await aiService.generate({
        topic: values.topic,
        contentType: values.contentType || currentContentType || 'ARTICLE',
        tone: values.tone || 'Professional',
        length: values.length || 'MEDIUM',
        audience: values.audience || 'General',
        instructions: values.instructions,
        contentId,
      });
      setGenResult(res);
      loadUsage();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Generation failed. Existing content was not modified.');
    } finally {
      setLoading(false);
    }
  };

  // 2. Rewrite Handler
  const handleRewrite = async (values: any) => {
    if (!values.content?.trim()) {
      message.warning('Please provide content to rewrite');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await aiService.rewrite({
        content: values.content,
        tone: values.tone || 'Professional',
        style: values.style || 'Concise',
        instructions: values.instructions,
        contentId,
      });
      setRewriteResult(res);
      loadUsage();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Rewrite failed.');
    } finally {
      setLoading(false);
    }
  };

  // 3. Summarize Handler
  const handleSummarize = async () => {
    if (!currentBody?.trim()) {
      message.warning('Content editor body is currently empty');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await aiService.summarize({
        content: currentBody,
        contentId,
      });
      setSummarizeResult(res);
      loadUsage();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Summarization failed.');
    } finally {
      setLoading(false);
    }
  };

  // 4. Generate Titles Handler
  const handleGenerateTitles = async () => {
    const input = currentTitle || currentBody;
    if (!input?.trim()) {
      message.warning('Please enter a title or body text first');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await aiService.generateTitles({
        topicOrContent: input,
        count: 5,
        contentId,
      });
      setTitlesResult(res);
      loadUsage();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Title generation failed.');
    } finally {
      setLoading(false);
    }
  };

  // 5. Generate SEO Handler
  const handleGenerateSEO = async () => {
    if (!currentBody?.trim() && !currentTitle?.trim()) {
      message.warning('Please provide content or a title first');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await aiService.generateSEO({
        content: currentBody || currentTitle,
        title: currentTitle,
        excerpt: currentExcerpt,
        contentId,
      });
      setSeoResult(res);
      loadUsage();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'SEO generation failed.');
    } finally {
      setLoading(false);
    }
  };

  // 6. Analyze Content Handler
  const handleAnalyze = async () => {
    if (!currentBody?.trim()) {
      message.warning('Please enter content to analyze in the editor');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await aiService.analyze({
        content: currentBody,
        title: currentTitle,
        contentId,
      });
      setAnalyzeResult(res);
      loadUsage();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Content analysis failed.');
    } finally {
      setLoading(false);
    }
  };

  // Helper to map suggested tag strings to existing tag IDs if matching
  const findMatchingTagIds = (tagNames: string[]) => {
    if (!tagNames || !availableTags) return [];
    return availableTags
      .filter(t => tagNames.some(name => name.toLowerCase() === t.name.toLowerCase() || name.toLowerCase() === t.slug.toLowerCase()))
      .map(t => t.id);
  };

  return (
    <Drawer
      title={
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space>
            <RobotOutlined style={{ fontSize: 20, color: '#1677ff' }} />
            <Text strong style={{ fontSize: 16 }}>AI Content Intelligence</Text>
          </Space>
          {usage && (
            <Tooltip title={`Daily quota for ${usage.role}: ${usage.usedToday} / ${usage.dailyLimit}`}>
              <Tag color={usage.remainingToday > 10 ? 'blue' : 'warning'} style={{ borderRadius: 12 }}>
                ⚡ {usage.remainingToday} requests left
              </Tag>
            </Tooltip>
          )}
        </Space>
      }
      placement="right"
      width={620}
      onClose={onClose}
      open={visible}
      bodyStyle={{ padding: '16px 20px', backgroundColor: '#fafbfc' }}
    >
      {error && (
        <Alert
          message="AI Operation Notice"
          description={error}
          type="error"
          showIcon
          closable
          onClose={() => setError(null)}
          style={{ marginBottom: 16 }}
        />
      )}

      <Tabs
        activeKey={activeTab}
        onChange={(k) => {
          setActiveTab(k);
          setError(null);
        }}
        items={[
          {
            key: 'generate',
            label: (
              <span><ThunderboltOutlined /> Generate</span>
            ),
            children: (
              <div>
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Form form={genForm} layout="vertical" onFinish={handleGenerate}>
                    <Form.Item
                      name="topic"
                      label="Topic / Subject"
                      rules={[{ required: true, message: 'Please enter topic' }]}
                    >
                      <Input placeholder="e.g. Next-Generation Cloud Observability" />
                    </Form.Item>

                    <Row gutter={12}>
                      <Col span={12}>
                        <Form.Item name="tone" label="Tone" initialValue="Professional">
                          <Select>
                            <Option value="Professional">Professional</Option>
                            <Option value="Engaging">Engaging & Lively</Option>
                            <Option value="Authoritative">Authoritative</Option>
                            <Option value="Conversational">Conversational</Option>
                            <Option value="Inspirational">Inspirational</Option>
                          </Select>
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item name="length" label="Length" initialValue="MEDIUM">
                          <Select>
                            <Option value="SHORT">Short (~300 words)</Option>
                            <Option value="MEDIUM">Medium (~600 words)</Option>
                            <Option value="LONG">Long In-Depth (~1200 words)</Option>
                          </Select>
                        </Form.Item>
                      </Col>
                    </Row>

                    <Row gutter={12}>
                      <Col span={12}>
                        <Form.Item name="audience" label="Target Audience" initialValue="Developers">
                          <Input placeholder="e.g. Executives, Developers" />
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item name="contentType" label="Content Type" initialValue={currentContentType || 'ARTICLE'}>
                          <Select>
                            <Option value="ARTICLE">Article</Option>
                            <Option value="BLOG_POST">Blog Post</Option>
                            <Option value="NEWS">News</Option>
                            <Option value="SOCIAL_POST">Social Post</Option>
                            <Option value="DOCUMENT">Document</Option>
                            <Option value="ANNOUNCEMENT">Announcement</Option>
                          </Select>
                        </Form.Item>
                      </Col>
                    </Row>

                    <Form.Item name="instructions" label="Custom Instructions (Optional)">
                      <TextArea rows={2} placeholder="e.g. Include architectural checklist and bullet points" />
                    </Form.Item>

                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      icon={<ThunderboltOutlined />}
                      block
                      style={{ height: 40, background: 'linear-gradient(135deg, #1677ff 0%, #722ed1 100%)' }}
                    >
                      {loading ? 'AI is drafting content...' : 'Generate Content'}
                    </Button>
                  </Form>
                </Card>

                {genResult && (
                  <Card
                    title={
                      <Space>
                        <CheckOutlined style={{ color: '#52c41a' }} />
                        <Text strong>AI Generated Content</Text>
                      </Space>
                    }
                    extra={
                      <Space>
                        <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopy(genResult.content)}>
                          Copy
                        </Button>
                        <Button
                          size="small"
                          icon={<ReloadOutlined />}
                          onClick={() => handleGenerate(genForm.getFieldsValue())}
                        >
                          Regenerate
                        </Button>
                      </Space>
                    }
                    style={{ border: '1px solid #d9d9d9', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
                  >
                    <div style={{ marginBottom: 12 }}>
                      <Text type="secondary">Generated Title:</Text>
                      <Paragraph strong style={{ fontSize: 16 }}>{genResult.title}</Paragraph>
                    </div>

                    <div style={{ marginBottom: 12 }}>
                      <Text type="secondary">Excerpt:</Text>
                      <Paragraph italic>{genResult.excerpt}</Paragraph>
                    </div>

                    <div style={{ marginBottom: 12 }}>
                      <Text type="secondary">Tags:</Text>
                      <div style={{ marginTop: 4 }}>
                        {genResult.suggestedTags?.map((tag, i) => (
                          <Tag color="cyan" key={i}>{tag}</Tag>
                        ))}
                      </div>
                    </div>

                    <Divider style={{ margin: '12px 0' }} />

                    <div style={{ maxHeight: 250, overflowY: 'auto', background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
                      <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', margin: 0 }}>
                        {genResult.content}
                      </pre>
                    </div>

                    <Divider style={{ margin: '16px 0' }} />

                    <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
                      <Button
                        type="primary"
                        icon={<CheckOutlined />}
                        onClick={() => {
                          const matchedTagIds = findMatchingTagIds(genResult.suggestedTags);
                          onApplyGenerated({
                            title: genResult.title,
                            body: genResult.content,
                            excerpt: genResult.excerpt,
                            tagIds: matchedTagIds.length > 0 ? matchedTagIds : undefined,
                          });
                          message.success('Applied all AI content into editor form');
                        }}
                      >
                        Accept All Fields
                      </Button>
                      <Button
                        icon={<PlusOutlined />}
                        onClick={() => {
                          onInsertContent(genResult.content);
                          message.success('Inserted into body');
                        }}
                      >
                        Insert into Body
                      </Button>
                      <Button
                        danger
                        icon={<CloseOutlined />}
                        onClick={() => setGenResult(null)}
                      >
                        Discard
                      </Button>
                    </Space>
                  </Card>
                )}
              </div>
            ),
          },
          {
            key: 'rewrite',
            label: (
              <span><FormOutlined /> Rewrite</span>
            ),
            children: (
              <div>
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Form form={rewriteForm} layout="vertical" onFinish={handleRewrite}>
                    <Form.Item
                      name="content"
                      label="Content to Rewrite"
                      rules={[{ required: true, message: 'Please provide text to rewrite' }]}
                      extra="Paste highlighted text or the full draft to rephrase."
                    >
                      <TextArea rows={4} placeholder="Paste draft or selection..." />
                    </Form.Item>

                    <Row gutter={12}>
                      <Col span={12}>
                        <Form.Item name="tone" label="Target Tone" initialValue="Engaging">
                          <Select>
                            <Option value="Professional">Professional</Option>
                            <Option value="Engaging">Engaging</Option>
                            <Option value="Persuasive">Persuasive</Option>
                            <Option value="Academic">Academic</Option>
                            <Option value="Casual">Casual</Option>
                          </Select>
                        </Form.Item>
                      </Col>
                      <Col span={12}>
                        <Form.Item name="style" label="Style" initialValue="Concise">
                          <Select>
                            <Option value="Concise">Concise & Punchy</Option>
                            <Option value="Elaborate">Elaborate & Detailed</Option>
                            <Option value="Bullet-points">Bullet Points</Option>
                            <Option value="Storytelling">Narrative / Storytelling</Option>
                          </Select>
                        </Form.Item>
                      </Col>
                    </Row>

                    <Form.Item name="instructions" label="Specific Instructions (Optional)">
                      <Input placeholder="e.g. Focus on technical credibility, eliminate jargon" />
                    </Form.Item>

                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      icon={<SwapOutlined />}
                      block
                      style={{ height: 38 }}
                    >
                      Rewrite Content
                    </Button>
                  </Form>
                </Card>

                {rewriteResult && (
                  <Card
                    title="Rewritten Output"
                    size="small"
                    extra={
                      <Space>
                        <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopy(rewriteResult.rewrittenContent)}>
                          Copy
                        </Button>
                        <Button size="small" icon={<ReloadOutlined />} onClick={() => handleRewrite(rewriteForm.getFieldsValue())}>
                          Regenerate
                        </Button>
                      </Space>
                    }
                  >
                    <div style={{ maxHeight: 220, overflowY: 'auto', background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0', marginBottom: 16 }}>
                      <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', margin: 0 }}>
                        {rewriteResult.rewrittenContent}
                      </pre>
                    </div>

                    <Space wrap>
                      <Button
                        type="primary"
                        icon={<CheckOutlined />}
                        onClick={() => {
                          onReplaceBody(rewriteResult.rewrittenContent);
                          message.success('Replaced body with rewritten content');
                        }}
                      >
                        Replace Body
                      </Button>
                      <Button
                        icon={<PlusOutlined />}
                        onClick={() => {
                          onInsertContent(rewriteResult.rewrittenContent);
                          message.success('Appended rewritten content');
                        }}
                      >
                        Insert at End
                      </Button>
                      <Button danger onClick={() => setRewriteResult(null)}>
                        Cancel
                      </Button>
                    </Space>
                  </Card>
                )}
              </div>
            ),
          },
          {
            key: 'summarize',
            label: (
              <span><FileTextOutlined /> Summarize</span>
            ),
            children: (
              <div>
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Paragraph type="secondary">
                    Extract an executive summary and high-impact key takeaway bullet points directly from the current editor body.
                  </Paragraph>
                  <Button
                    type="primary"
                    onClick={handleSummarize}
                    loading={loading}
                    icon={<FileTextOutlined />}
                    block
                    style={{ height: 38 }}
                  >
                    Summarize Current Content
                  </Button>
                </Card>

                {summarizeResult && (
                  <Card
                    title="Summary & Key Points"
                    size="small"
                    extra={
                      <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopy(summarizeResult.summary)}>
                        Copy
                      </Button>
                    }
                  >
                    <div style={{ marginBottom: 12 }}>
                      <Text strong>Executive Summary:</Text>
                      <Paragraph style={{ marginTop: 4 }}>{summarizeResult.summary}</Paragraph>
                    </div>

                    <div>
                      <Text strong>Key Takeaways:</Text>
                      <ul style={{ paddingLeft: 20, marginTop: 6 }}>
                        {summarizeResult.keyPoints?.map((pt, i) => (
                          <li key={i}>{pt}</li>
                        ))}
                      </ul>
                    </div>

                    <Divider style={{ margin: '12px 0' }} />

                    <Space wrap>
                      <Button
                        type="primary"
                        onClick={() => {
                          onApplyGenerated({ excerpt: summarizeResult.summary });
                          message.success('Applied summary to Excerpt field');
                        }}
                      >
                        Use as Excerpt
                      </Button>
                      <Button
                        onClick={() => {
                          const formatted = `## Summary\n${summarizeResult.summary}\n\n### Key Takeaways\n${summarizeResult.keyPoints.map(p => `- ${p}`).join('\n')}\n`;
                          onInsertContent(formatted);
                          message.success('Inserted summary into body');
                        }}
                      >
                        Insert into Body
                      </Button>
                    </Space>
                  </Card>
                )}
              </div>
            ),
          },
          {
            key: 'titles',
            label: (
              <span><CompassOutlined /> Titles</span>
            ),
            children: (
              <div>
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Paragraph type="secondary">
                    Generate viral, SEO-focused headline options using your current title or body as context.
                  </Paragraph>
                  <Button
                    type="primary"
                    onClick={handleGenerateTitles}
                    loading={loading}
                    icon={<CompassOutlined />}
                    block
                    style={{ height: 38 }}
                  >
                    Generate Title Suggestions
                  </Button>
                </Card>

                {titlesResult && (
                  <Card title="Headline Suggestions" size="small">
                    {titlesResult.titles?.map((titleOption, idx) => (
                      <div
                        key={idx}
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          padding: '10px 12px',
                          marginBottom: 8,
                          borderRadius: 6,
                          background: '#fff',
                          border: '1px solid #f0f0f0',
                        }}
                      >
                        <Text strong style={{ flex: 1, marginRight: 12 }}>{titleOption}</Text>
                        <Button
                          size="small"
                          type="primary"
                          ghost
                          onClick={() => {
                            onApplyTitle(titleOption);
                            message.success('Applied title to editor');
                          }}
                        >
                          Use Title
                        </Button>
                      </div>
                    ))}
                  </Card>
                )}
              </div>
            ),
          },
          {
            key: 'seo',
            label: (
              <span><SearchOutlined /> SEO</span>
            ),
            children: (
              <div>
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Paragraph type="secondary">
                    Analyze content and generate high-impact SEO title, meta description, keywords, and URL slug.
                  </Paragraph>
                  <Button
                    type="primary"
                    onClick={handleGenerateSEO}
                    loading={loading}
                    icon={<SearchOutlined />}
                    block
                    style={{ height: 38 }}
                  >
                    Generate SEO Metadata
                  </Button>
                </Card>

                {seoResult && (
                  <Card
                    title="SEO Recommendations"
                    size="small"
                    extra={
                      <Button
                        type="primary"
                        size="small"
                        icon={<CheckOutlined />}
                        onClick={() => {
                          onApplySeo({
                            seoTitle: seoResult.seoTitle,
                            seoDescription: seoResult.metaDescription,
                            slug: seoResult.slugSuggestion,
                          });
                          message.success('Applied SEO fields to editor form');
                        }}
                      >
                        Apply All SEO Fields
                      </Button>
                    }
                  >
                    <div style={{ marginBottom: 12 }}>
                      <Text strong>SEO Title:</Text>
                      <Paragraph style={{ margin: 0 }}>{seoResult.seoTitle}</Paragraph>
                      <Text type="secondary" style={{ fontSize: 12 }}>{seoResult.seoTitle.length} / 60 characters</Text>
                    </div>

                    <div style={{ marginBottom: 12 }}>
                      <Text strong>Meta Description:</Text>
                      <Paragraph style={{ margin: 0 }}>{seoResult.metaDescription}</Paragraph>
                      <Text type="secondary" style={{ fontSize: 12 }}>{seoResult.metaDescription.length} / 160 characters</Text>
                    </div>

                    <div style={{ marginBottom: 12 }}>
                      <Text strong>URL Slug Suggestion:</Text>
                      <Paragraph code style={{ margin: 0 }}>{seoResult.slugSuggestion}</Paragraph>
                    </div>

                    <div>
                      <Text strong>Keywords:</Text>
                      <div style={{ marginTop: 4 }}>
                        {seoResult.keywords?.map((kw, i) => (
                          <Tag color="geekblue" key={i}>{kw}</Tag>
                        ))}
                      </div>
                    </div>
                  </Card>
                )}
              </div>
            ),
          },
          {
            key: 'analyze',
            label: (
              <span><BarChartOutlined /> Audit & Analyze</span>
            ),
            children: (
              <div>
                <Card size="small" style={{ marginBottom: 16 }}>
                  <Paragraph type="secondary">
                    Audit the current body for readability, structure, SEO completeness, tone consistency, and potential repetition.
                  </Paragraph>
                  <Button
                    type="primary"
                    onClick={handleAnalyze}
                    loading={loading}
                    icon={<BarChartOutlined />}
                    block
                    style={{ height: 38 }}
                  >
                    Run Content Quality Audit
                  </Button>
                </Card>

                {analyzeResult && (
                  <Card title="Content Quality Audit Report" size="small">
                    <Row gutter={16} style={{ marginBottom: 16 }}>
                      <Col span={8} style={{ textAlign: 'center' }}>
                        <Progress
                          type="circle"
                          percent={analyzeResult.readabilityScore}
                          size={70}
                          status={analyzeResult.readabilityScore > 80 ? 'success' : 'normal'}
                        />
                        <div style={{ marginTop: 6 }}>
                          <Text strong style={{ fontSize: 12 }}>Readability</Text>
                          <br />
                          <Text type="secondary" style={{ fontSize: 11 }}>{analyzeResult.readabilityGrade}</Text>
                        </div>
                      </Col>
                      <Col span={8} style={{ textAlign: 'center' }}>
                        <Progress
                          type="circle"
                          percent={analyzeResult.structureScore}
                          size={70}
                          status={analyzeResult.structureScore > 80 ? 'success' : 'normal'}
                        />
                        <div style={{ marginTop: 6 }}>
                          <Text strong style={{ fontSize: 12 }}>Structure</Text>
                        </div>
                      </Col>
                      <Col span={8} style={{ textAlign: 'center' }}>
                        <Progress
                          type="circle"
                          percent={analyzeResult.seoCompletenessScore}
                          size={70}
                          status={analyzeResult.seoCompletenessScore > 80 ? 'success' : 'normal'}
                        />
                        <div style={{ marginTop: 6 }}>
                          <Text strong style={{ fontSize: 12 }}>SEO Completeness</Text>
                        </div>
                      </Col>
                    </Row>

                    <Divider style={{ margin: '12px 0' }} />

                    <div style={{ marginBottom: 12 }}>
                      <Text strong>Structure Assessment:</Text>
                      <Paragraph type="secondary">{analyzeResult.structureAnalysis}</Paragraph>
                    </div>

                    <div style={{ marginBottom: 12 }}>
                      <Text strong>SEO & Tone Assessment:</Text>
                      <Paragraph type="secondary">{analyzeResult.seoCompleteness}</Paragraph>
                      <Paragraph type="secondary"><Text strong>Tone:</Text> {analyzeResult.toneConsistency}</Paragraph>
                    </div>

                    {analyzeResult.potentialRepetition?.length > 0 && (
                      <div style={{ marginBottom: 12 }}>
                        <Text strong style={{ color: '#d46b08' }}>Repetition Alerts:</Text>
                        <ul style={{ paddingLeft: 20, marginTop: 4 }}>
                          {analyzeResult.potentialRepetition.map((rep, idx) => (
                            <li key={idx} style={{ color: '#d46b08' }}>{rep}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {analyzeResult.missingSections?.length > 0 && (
                      <div style={{ marginBottom: 12 }}>
                        <Text strong>Suggested Missing Sections:</Text>
                        <ul style={{ paddingLeft: 20, marginTop: 4 }}>
                          {analyzeResult.missingSections.map((sec, idx) => (
                            <li key={idx}>{sec}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    {analyzeResult.recommendations?.length > 0 && (
                      <div>
                        <Text strong style={{ color: '#1677ff' }}>Editorial Recommendations:</Text>
                        <ul style={{ paddingLeft: 20, marginTop: 4 }}>
                          {analyzeResult.recommendations.map((rec, idx) => (
                            <li key={idx}><Text strong>{rec}</Text></li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </Card>
                )}
              </div>
            ),
          },
        ]}
      />
    </Drawer>
  );
};
