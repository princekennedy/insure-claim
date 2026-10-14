'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from './card';
import { Button } from './button';
import { getChatConfig, createConversation, sendMessage } from '@/lib/services/chat';
import { ApiError } from '@/lib/services/api';
import type { ChatConfig } from '@/lib/types';
import styles from './chatbot-card.module.css';

interface ChatbotCardProps {
  className?: string;
  onOpenChat?: () => void;
  /** Optional claim the conversation should be bound to. */
  claimId?: number;
}

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  error?: boolean;
}

const FALLBACK_GREETING =
  "Hello! I'm the Britam claims assistant. How can I help with your insurance claims today?";

export function ChatbotCard({ className = '', onOpenChat, claimId }: ChatbotCardProps) {
  const [isHovered, setIsHovered] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [config, setConfig] = useState<ChatConfig | null>(null);
  const [configLoaded, setConfigLoaded] = useState(false);
  const [conversationId, setConversationId] = useState<number | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [escalated, setEscalated] = useState(false);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = useCallback(() => {
    if (messagesContainerRef.current) {
      messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
    }
  }, []);

  // Load the public chatbot config once on mount. If it can't be reached we
  // optimistically allow the widget; the send call surfaces any failure.
  useEffect(() => {
    let cancelled = false;
    getChatConfig()
      .then((data) => {
        if (cancelled) return;
        setConfig(data);
        setMessages([
          { id: 'greeting', role: 'assistant', content: data.greeting, timestamp: new Date() },
        ]);
      })
      .catch(() => {
        if (cancelled) return;
        setConfig({ enabled: true, mode: 'demo', model: null, greeting: FALLBACK_GREETING });
        setMessages([
          { id: 'greeting', role: 'assistant', content: FALLBACK_GREETING, timestamp: new Date() },
        ]);
      })
      .finally(() => {
        if (!cancelled) setConfigLoaded(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, isChatOpen, scrollToBottom]);

  const isEnabled = config?.enabled ?? false;

  const handleChatClick = (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isEnabled) return;
    setIsChatOpen(true);
    onOpenChat?.();
  };

  const ensureConversation = async (): Promise<number> => {
    if (conversationId !== null) return conversationId;
    const conversation = await createConversation(claimId ? { claimId } : {});
    setConversationId(conversation.id);
    return conversation.id;
  };

  const handleSendMessage = async () => {
    const content = inputValue.trim();
    if (!content || isTyping || !isEnabled) return;

    const userMessage: Message = {
      id: `u-${Date.now()}`,
      role: 'user',
      content,
      timestamp: new Date(),
    };
    setMessages((prev) => [...prev, userMessage]);
    setInputValue('');
    setIsTyping(true);

    try {
      const id = await ensureConversation();
      const result = await sendMessage(id, { content });
      setMessages((prev) => [
        ...prev,
        {
          id: `a-${result.assistantMessage.id}`,
          role: 'assistant',
          content: result.assistantMessage.content,
          timestamp: new Date(result.assistantMessage.createdAt),
        },
      ]);
      if (result.escalated) setEscalated(true);
    } catch (err) {
      if (err instanceof ApiError && err.data && (err.data as { code?: string }).code === 'CHATBOT_DISABLED') {
        setConfig((prev) => (prev ? { ...prev, enabled: false } : prev));
      }
      setMessages((prev) => [
        ...prev,
        {
          id: `e-${Date.now()}`,
          role: 'assistant',
          content:
            "Sorry, I couldn't reach the assistant just now. Please try again in a moment.",
          timestamp: new Date(),
          error: true,
        },
      ]);
    } finally {
      setIsTyping(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void handleSendMessage();
    }
  };

  return (
    <div
      className={`${styles.chatbotCard} ${className}`}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <Card hover>
        {!isChatOpen ? (
          <>
            <CardHeader className={styles.header}>
              <div className={styles.iconContainer}>
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  className={styles.chatIcon}
                >
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                </svg>
              </div>
              <CardTitle>AI Assistant</CardTitle>
              <CardDescription>
                {!configLoaded
                  ? 'Connecting…'
                  : isEnabled
                    ? 'Get instant help with claims, policy questions, and support'
                    : 'The assistant is offline right now. Please try again later.'}
              </CardDescription>
            </CardHeader>
            <CardContent className={styles.content}>
              <div className={styles.features}>
                <div className={styles.feature}>
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    width="16"
                    height="16"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <polyline points="12 6 12 12 16 14" />
                  </svg>
                  <span>24/7 Available</span>
                </div>
                <div className={styles.feature}>
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    width="16"
                    height="16"
                  >
                    <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
                    <polyline points="13 2 13 9 20 9" />
                  </svg>
                  <span>Instant Responses</span>
                </div>
                <div className={styles.feature}>
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    width="16"
                    height="16"
                  >
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  </svg>
                  <span>Secure & Private</span>
                </div>
              </div>
              <Button
                className={styles.chatButton}
                onClick={handleChatClick}
                disabled={!configLoaded || !isEnabled}
                fullWidth
              >
                {!configLoaded ? 'Connecting…' : !isEnabled ? 'Unavailable' : isHovered ? 'Start Chatting' : 'Chat Now'}
              </Button>
            </CardContent>
          </>
        ) : (
          <CardContent className={styles.chatContent}>
            <div className={styles.chatHeader}>
              <div className={styles.chatHeaderLeft}>
                <div className={styles.iconContainerSmall}>
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    className={styles.chatIconSmall}
                  >
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                </div>
                <div>
                  <CardTitle className={styles.chatTitle}>AI Assistant</CardTitle>
                  <CardDescription className={styles.chatDescription}>
                    {config?.mode === 'live' ? 'Online' : 'Online · Demo'}
                  </CardDescription>
                </div>
              </div>
              <Button
                className={styles.closeButton}
                onClick={() => setIsChatOpen(false)}
                variant="ghost"
              >
                <svg viewBox="0 0 20 20" fill="currentColor" width="20" height="20">
                  <path
                    fillRule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                    clipRule="evenodd"
                  />
                </svg>
              </Button>
            </div>
            <div className={styles.messagesContainer} ref={messagesContainerRef}>
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`${styles.message} ${styles[message.role]}`}
                >
                  <div
                    className={`${styles.messageContent} ${message.error ? styles.messageError : ''}`}
                  >
                    {message.content}
                  </div>
                  <span className={styles.messageTime}>
                    {message.timestamp.toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
              ))}
              {isTyping && (
                <div className={`${styles.message} ${styles.assistant}`}>
                  <div className={styles.messageContent}>
                    <div className={styles.typingIndicator}>
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
            {escalated && (
              <div className={styles.notice}>
                A Britam specialist has been notified and will follow up with you.
              </div>
            )}
            <div className={styles.inputContainer}>
              <textarea
                className={styles.chatInput}
                placeholder="Type your message..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={!isEnabled}
                rows={1}
              />
              <Button
                className={styles.sendButton}
                onClick={() => void handleSendMessage()}
                disabled={!inputValue.trim() || isTyping || !isEnabled}
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  width="20"
                  height="20"
                >
                  <line x1="22" y1="2" x2="11" y2="13" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
              </Button>
            </div>
          </CardContent>
        )}
      </Card>
    </div>
  );
}