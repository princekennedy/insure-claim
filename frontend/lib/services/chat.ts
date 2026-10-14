import { request } from './api';
import type {
  ChatConfig,
  Conversation,
  ConversationDetail,
  CreateConversationRequest,
  ChatSendResult,
  SendMessageRequest,
} from '../types';

export function getChatConfig(): Promise<ChatConfig> {
  return request<ChatConfig>('/chat/config');
}

export function createConversation(body: CreateConversationRequest = {}): Promise<Conversation> {
  return request<Conversation>('/chat/conversations', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function getConversation(conversationId: number): Promise<ConversationDetail> {
  return request<ConversationDetail>(`/chat/conversations/${conversationId}`);
}

export function sendMessage(
  conversationId: number,
  body: SendMessageRequest,
): Promise<ChatSendResult> {
  return request<ChatSendResult>(`/chat/conversations/${conversationId}/messages`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}