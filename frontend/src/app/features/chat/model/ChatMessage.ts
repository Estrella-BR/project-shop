export interface ChatMessage {
  id: number;
  content: string;
  role: 'user' | 'assistant';
  timestamp: Date;
  loading?: boolean;
}