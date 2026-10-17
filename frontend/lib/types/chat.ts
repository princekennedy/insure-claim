export interface ChatConfig {
  enabled: boolean;
  mode: 'live' | 'demo';
  model: string | null;
  greeting: string;
}

export interface Conversation {
  id: number;
  reference: string;
  status: ChatStatus;
  escalated: boolean;
  messagesCount: number;
  startedAt: string;
  escalatedAt: string | null;
}

export type ChatStatus = 'OPEN' | 'ESCALATED' | 'RESOLVED' | 'CLOSED';
export type ChatSender = 'CUSTOMER' | 'ASSISTANT' | 'AGENT';

export interface ChatMessage {
  id: number;
  sender: ChatSender;
  content: string;
  intent: string | null;
  confidence: number | null;
  escalateSuggested: boolean;
  createdAt: string;
}

export interface SupportConcern {
  id: number;
  category: string;
  severity: string;
  summary: string;
  status: string;
  conversationId: number | null;
  createdAt: string;
}

export interface CreateConversationRequest {
  claimId?: number | null;
}

export interface SendMessageRequest {
  content: string;
}

export interface ConversationDetail {
  conversation: Conversation;
  messages: ChatMessage[];
}

export interface ChatSendResult {
  conversation: Conversation;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
  escalated: boolean;
  concern: SupportConcern | null;
}