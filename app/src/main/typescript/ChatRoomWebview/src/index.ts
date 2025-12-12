import "./Components/WebviewControllerElement"
import { IAndroidBridge, IAsyncAndroidBridge, IPromiser, IWebviewController, IWebviewControllerDecoder, IWebviewControllerEncoder } from "./Interfaces/IWebviewController";
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



