import { ChatMessage, ChatMessageHistoryRequest, ChatRoom } from "../Generated/chat";
import { IChatMessage } from "./IChatMessage";
import { IChatRoom } from "./IChatRoom";
import type { IElementConvertable } from "./IElementConvertable";

export type Base64String = string;

// Represents the interface between the wider app and the chat webview
export interface IWebviewController extends IElementConvertable {
    addRoom(room: IChatRoom): boolean;
    removeRoom(roomId: string): boolean;
    switchToRoom(roomId: string): boolean;

    addMessage(message: ChatMessage): boolean;
    addMessages(...messages: ChatMessage[]): boolean;
    removeMessage(messageId: string, roomId: string) : boolean;
    removeMessages(...messageIds: [messageId: string, roomId: string][]): boolean;
    hasMessage(messageId: string, roomId?: string): boolean;
    getMessage(messageId: string, roomId?: string) : ChatMessage | null;
}

export interface IWebviewControllerDecoder {
    decodeChatRoom(b64: string): ChatRoom;
    decodeChatMessage(b64: string): ChatMessage;
    decodeChatMessageHistoryRequestResponse(b64: string): ChatMessage[];
    toByteArray(b64: string): Uint8Array;
}

export interface IWebviewControllerEncoder { 
    encodeChatRoom(room: ChatRoom): string;
    encodeChatMessage(message: ChatMessage): string;
    encodeChatMessageHistoryRequest(request: ChatMessageHistoryRequest): string;
}