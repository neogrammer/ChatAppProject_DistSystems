import "./Components/WebviewControllerElement"
import { ChatMessage, GroupInfo, GetMessagesRequest, GetMessagesResponse, GetUserGroupsRequest, GetUserGroupsResponse, CreateGroupResponse, SearchUsersResponse } from "./Generated/chat";
import { IWebviewController, IWebviewControllerDecoder, IWebviewControllerEncoder } from "./Interfaces/IWebviewController";
import * as signalR from "@microsoft/signalr";
import "./base.css";

declare global {
  const DEBUG: boolean;
  const AndroidBridge: IAndroidBridge;
  const WebviewController: IWebviewController;
  const WebviewControllerDecoder: IWebviewControllerDecoder;
  const WebviewControllerEncoder: IWebviewControllerEncoder;
  const DLOG: (val: any) => void;
  const connection: signalR.HubConnection;
  const AsyncAndroidBridge: IAsyncAndroidBridge;
  const Promiser: IPromiser;

  interface IAndroidBridge {
    // Returns the current user's name and ID
    getUserName(): string;

    // Returns the current user's ID
    getUserId(): string;

    // Called when the webview has finished loading
    setLoaded(): void;

    // Shows or hides a loading dialog
    showLoadingDialog(): void;
    hideLoadingDialog(): void;

    // use WebviewControllerEncoder.encodeChatMessage(message) to convert ChatMessage to base64
    postMessage(ChatMessage_b64: string): void;

    // use WebviewControllerEncoder.encodeChatMessageHistoryRequest(request) to convert GetMessagesRequest to base64
    // requestMessageHistory(GetMessagesRequest_b64: string): void;
  }

  interface IAsyncAndroidBridge {
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
  interface IPromiser {
    // Returns a new promise that's registered under the given ID
    registerNewPromise<T>(id: string): Promise<T>;

    // Resolves the promise with the given value and ID
    resolvePromise<T>(value: T, id: string): void;

    // Rejects the promise with the given reason and ID
    rejectPromise<T>(reason: any, id: string): void;
  }

  interface Window {
    DEBUG: boolean;
    DLOG: (val: any) => void
    AndroidBridge: IAndroidBridge;
    WebviewController: IWebviewController;
    WebviewControllerDecoder: IWebviewControllerDecoder;
    WebviewControllerEncoder: IWebviewControllerEncoder;
    connection: signalR.HubConnection
    AsyncAndroidBridge: IAsyncAndroidBridge;
    Promiser: IPromiser;
  }
}



