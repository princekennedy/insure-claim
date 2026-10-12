'use client';

import { useState, useRef, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from './card';
import { Button } from './button';
import styles from './chatbot-card.module.css';

interface ChatbotCardProps {
  className?: string;
  onOpenChat?: () => void;
}

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export function ChatbotCard({ className = '', onOpenChat }: ChatbotCardProps) {
  const [isHovered, setIsHovered] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      role: 'assistant',
      content: 'Hello! I\'m your AI assistant. How can I help you with your insurance claims today?',
      timestamp: new Date(),
    },
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    if (messagesContainerRef.current) {
      messagesContainerRef.current.scrollTop = messagesContainerRef.current.scrollHeight;
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isChatOpen]);

  const handleChatClick = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsChatOpen(true);
    if (onOpenChat) {
      onOpenChat();
    }
  };

  const handleSendMessage = () => {
    if (!inputValue.trim()) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: inputValue,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputValue('');
    setIsTyping(true);

    // Simulate AI response
    setTimeout(() => {
      const aiResponse: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: 'Thank you for your message. I\'m here to help you with your insurance claims, policy questions, and any other support you need. Could you please provide more details about what you\'d like assistance with?',
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, aiResponse]);
      setIsTyping(false);
    }, 1500);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
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
                Get instant help with claims, policy questions, and support
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
                fullWidth
              >
                {isHovered ? 'Start Chatting' : 'Chat Now'}
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
                    Online
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
                  <div className={styles.messageContent}>
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
            <div className={styles.inputContainer}>
              <textarea
                className={styles.chatInput}
                placeholder="Type your message..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyPress={handleKeyPress}
                rows={1}
              />
              <Button
                className={styles.sendButton}
                onClick={handleSendMessage}
                disabled={!inputValue.trim() || isTyping}
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
