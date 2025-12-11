import { AddUserToGroupResponse, ChatMessage, CreateGroupResponse, GetMessagesRequest, GetUserGroupsRequest, GroupInfo, SearchUsersResponse } from "../Generated/chat";
import type { IElementConvertable } from "./IElementConvertable";

export type Base64String = string;

// Represents the interface between the wider app and the chat webview
export interface IWebviewController extends IElementConvertable {
    addRoom(room: GroupInfo): boolean;
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
    decodeChatRoom(b64: Base64String): GroupInfo;
    decodeChatMessage(b64: Base64String): ChatMessage;
    decodeChatMessageHistoryRequestResponse(b64: Base64String): ChatMessage[];
    decodeGetUserGroupsResponse(b64: Base64String): GroupInfo[];
    decodeSearchResult(b64: Base64String): SearchUsersResponse;
    decodeCreateGroupResponse(b64: Base64String): CreateGroupResponse;
    decodeAddUserToGroupResponse(b64: Base64String): boolean;
    toByteArray(b64: Base64String): Uint8Array;
}

export interface IWebviewControllerEncoder { 
    encodeGroupInfo(room: GroupInfo): Base64String;
    encodeChatMessage(message: ChatMessage): Base64String;
    encodeChatMessageHistoryRequest(request: GetMessagesRequest): Base64String;
    encodeGetUserGroupsRequest(request: GetUserGroupsRequest): Base64String;
}