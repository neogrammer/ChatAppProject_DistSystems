import "./Components/WebviewControllerElement"
import { ChatMessage, ChatMessageHistoryRequest, ChatMessageHistoryResponse, ChatRoom, MessageFns } from "./Generated/chat";
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

  interface IAndroidBridge {
    getUserName(): string;
    getUserId(): string;

    setLoaded(): void;

    // use WebviewControllerEncoder.encodeChatMessage(message) to convert ChatMessage to base64
    postMessage(ChatMessage_b64: string): void;

    // use WebviewControllerEncoder.encodeChatMessageHistoryRequest(request) to convert ChatMessageHistoryRequest to base64
    requestMessageHistory(ChatMessageHistoryRequest_b64: string): void;
  }

  interface Window {
    DEBUG: boolean;
    DLOG: (val: any) => void
    AndroidBridge: IAndroidBridge;
    WebviewController: IWebviewController;
    WebviewControllerDecoder: IWebviewControllerDecoder;
    WebviewControllerEncoder: IWebviewControllerEncoder;
  }
}

if(DEBUG) {
  function makeDummyMessage(id: string, roomId: string, userId: string, content: string, userName: string): ChatMessage {
    return {
      id: id,
      content: content,
      createdAt: Date.now(),
      roomId: roomId,
      userId: userId,
      userName: userName
    }
  }

  window.AndroidBridge ??= {
    getUserId() {
      return "0"
    },
    getUserName() {
      return "Tyler"
    },
    postMessage(b64) {
      
    },
    setLoaded() {
      
    },
    requestMessageHistory(ChatMessageHistoryRequest_b64) {
      
    },
  }

  window.DLOG = (val) => console.debug(val);

  const controller = document.querySelector('webview-controller');
  controller!.addRoom({id: "main", roomName: "Anonymous"});
  controller!.addRoom({id: "second", roomName: "Second Room"});
  //controller!.switchToRoom("0");
  controller!.addMessage(makeDummyMessage("0", "main", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));
  setTimeout(() => {
    controller!.addMessage(makeDummyMessage("1", "main", "2", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "User"));
  }, 1000);
  
}

else {
  window.DLOG = (val) => {}
}

customElements.whenDefined('webview-controller').then(() => {
  window['WebviewController'] = document.querySelector('webview-controller') as IWebviewController;
  window["WebviewControllerDecoder"] = {
    toByteArray(b64: string) {
      const bin = atob(b64);
      const len = bin.length;
      const bytes = new Uint8Array(len);
      for (let i = 0; i < len; i++) {
        bytes[i] = bin.charCodeAt(i);
      }
      return bytes;
    },
    decodeChatMessage(b64: string) {
      return ChatMessage.decode(this.toByteArray(b64));
    },
    decodeChatRoom(b64) {
      return ChatRoom.decode(this.toByteArray(b64));
    },
    decodeChatMessageHistoryRequestResponse(b64: string) { 
      return ChatMessageHistoryResponse.decode(this.toByteArray(b64)).messages;
    }
  }
  window["WebviewControllerEncoder"] = { 
    encodeChatMessage(message: ChatMessage) {
      return btoa(String.fromCharCode(...ChatMessage.encode(message).finish()));
    },
    encodeChatRoom(room: ChatRoom) {
      return btoa(String.fromCharCode(...ChatRoom.encode(room).finish()));
    },
    encodeChatMessageHistoryRequest(request: ChatMessageHistoryRequest) {
      return btoa(String.fromCharCode(...ChatMessageHistoryRequest.encode(request).finish()));
    },
  }

  const CHAT_SERVICE_URL = "http://10.0.2.2:5066";
  const ROOM_ID = "main";

  // setup signalr connection
  const connection = new signalR.HubConnectionBuilder()
    .withUrl(`${CHAT_SERVICE_URL}/chathub`)
    .withAutomaticReconnect()
    .build();

  // handle incoming messages
  connection.on("ReceiveMessage", (message: ChatMessage) => {
    DLOG("received message via signalr");
    DLOG(message);
    window.WebviewController.addMessage(message);
  });

  // connect and join room
  connection.start()
    .then(() => {
      DLOG("signalr connected");
      return connection.invoke("JoinGroup", ROOM_ID);
    })
    .then(() => {
      DLOG(`joined room: ${ROOM_ID}`);
    })
    .catch(err => {
      console.error("signalr connection error:", err);
    });

  AndroidBridge.setLoaded();
})



