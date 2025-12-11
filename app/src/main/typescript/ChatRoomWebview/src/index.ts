import "./Components/WebviewControllerElement"
import { ChatMessage, GroupInfo, GetMessagesRequest, GetMessagesResponse, GetUserGroupsRequest, GetUserGroupsResponse } from "./Generated/chat";
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
    getUserName(): string;
    getUserId(): string;

    setLoaded(): void;
    showLoadingDialog(): void;
    hideLoadingDialog(): void;

    // use WebviewControllerEncoder.encodeChatMessage(message) to convert ChatMessage to base64
    postMessage(ChatMessage_b64: string): void;

    // use WebviewControllerEncoder.encodeChatMessageHistoryRequest(request) to convert GetMessagesRequest to base64
    // requestMessageHistory(GetMessagesRequest_b64: string): void;
  }

  interface IAsyncAndroidBridge {
    requestMessageHistory(GetMessagesRequest_b64: string): Promise<ChatMessage[]>;
    requestUserGroups(): Promise<GroupInfo[]>;
  }

  interface IPromiser {
    registerNewPromise<T>(id: string): Promise<T>;
    resolvePromise<T>(value: T, id: string): void;
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



