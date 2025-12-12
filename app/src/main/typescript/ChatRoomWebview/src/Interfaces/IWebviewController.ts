import { AddUserToGroupResponse, ChatMessage, CreateGroupResponse, GetMessagesRequest, GetUserGroupsRequest, GroupInfo, SearchUsersResponse } from "../Generated/chat";
import type { IElementConvertable } from "./IElementConvertable";

export type Base64String = string;

// Represents the interface between the wider app and the chat webview, see WebviewControllerElement.ts for more documentation
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

// Decodes base 64 strings into their corresponding protobuf message types
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

// Encodes a protobuf object into a base 64 string
export interface IWebviewControllerEncoder { 
    encodeGroupInfo(room: GroupInfo): Base64String;
    encodeChatMessage(message: ChatMessage): Base64String;
    encodeChatMessageHistoryRequest(request: GetMessagesRequest): Base64String;
    encodeGetUserGroupsRequest(request: GetUserGroupsRequest): Base64String;
}

export interface IAndroidBridge {
    // Returns the current user's name and ID
    getUserName(): string;

    // Returns the current user's ID
    getUserId(): string;

    // Called when the webview has finished loading
    setLoaded(): void;

    // Shows or hides a loading dialog
    showLoadingDialog(): void;
    hideLoadingDialog(): void;

    // Shows a potentially recoverable error dialog
    showErrorDialog(title: string, message: string, recoverable: boolean): void;

    // use WebviewControllerEncoder.encodeChatMessage(message) to convert ChatMessage to base64
    postMessage(ChatMessage_b64: string): void;
}

export interface IAsyncAndroidBridge {
    // Returns a promise that resolves to an array of ChatMessages, pass in base64-encoded GetMessagesRequest (subject to change for optimization)
    requestMessageHistory(GetMessagesRequest_b64: string): Promise<ChatMessage[]>;

    // Returns a promise that resolves to an array of GroupInfo that represents the user's groups
    requestUserGroups(): Promise<GroupInfo[]>;

    // Returns a promise that resolves to an ISearchResult for users matching the substring
    searchUsers(substring: string): Promise<SearchUsersResponse>;

    // Creates a group with the given name with the current user as the initial user
    createGroup(name: string): Promise<CreateGroupResponse>;

    // Adds a user to a group.
    addUserToGroup(userId: string, groupId: string): Promise<boolean>;
}

  // Manages promises that are resolved/rejected from the native side using unique IDs and a registry for promises
export interface IPromiser {
    // Returns a new promise that's registered under the given ID
    registerNewPromise<T>(id: string): Promise<T>;

    // Resolves the promise with the given value and ID
    resolvePromise<T>(value: T, id: string): void;

    // Rejects the promise with the given reason and ID
    rejectPromise<T>(reason: any, id: string): void;
}