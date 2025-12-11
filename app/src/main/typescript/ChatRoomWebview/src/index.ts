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

  // window.AndroidBridge ??= {
  //   getUserId() {
  //     return "0"
  //   },
  //   getUserName() {
  //     return "Tyler"
  //   },
  //   postMessage(b64) {
      
  //   },
  //   setLoaded() {
      
  //   },
  //   requestMessageHistory(ChatMessageHistoryRequest_b64) {
      
  //   },
  //   showLoadingDialog() {},
  //   hideLoadingDialog() {}
  // }

  // const controller = document.querySelector('webview-controller');
  // controller!.addRoom({id: "main", roomName: "Anonymous"});
  // controller!.addRoom({id: "second", roomName: "Second Room"});
  // //controller!.switchToRoom("0");
  // controller!.addMessage(makeDummyMessage("0", "main", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));
  // setTimeout(() => {
  //   controller!.addMessage(makeDummyMessage("1", "main", "2", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "User"));
  //   controller!.addMessage(makeDummyMessage("2", "main", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));
  //   controller!.addMessage(makeDummyMessage("3", "main", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));

  // }, 1000);
  
  window.DLOG = (val) => console.debug(val);
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
      return GroupInfo.decode(this.toByteArray(b64));
    },
    decodeChatMessageHistoryRequestResponse(b64: string) { 
      return GetMessagesResponse.decode(this.toByteArray(b64)).messages;
    },
    decodeGetUserGroupsResponse(b64) {
      return GetUserGroupsResponse.decode(this.toByteArray(b64)).groups;
    },
  }
  window["WebviewControllerEncoder"] = { 
    encodeChatMessage(message: ChatMessage) {
      return btoa(String.fromCharCode(...ChatMessage.encode(message).finish()));
    },
    encodeGroupInfo(room: GroupInfo) {
      return btoa(String.fromCharCode(...GroupInfo.encode(room).finish()));
    },
    encodeChatMessageHistoryRequest(request: GetMessagesRequest) {
      return btoa(String.fromCharCode(...GetMessagesRequest.encode(request).finish()));
    },
    encodeGetUserGroupsRequest(request: GetUserGroupsRequest) {
      return btoa(String.fromCharCode(...GetUserGroupsRequest.encode(request).finish()));
    }
  }

  const CHAT_SERVICE_URL = "http://10.0.2.2:5066";
  //const ROOM_ID = "main";

  // setup signalr connection
  window.connection = new signalR.HubConnectionBuilder()
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
      //return connection.invoke("JoinGroup", ROOM_ID);
    })
    // .then(() => {
    //   DLOG(`joined room: ${ROOM_ID}`);
    // })
    .catch(err => {
      console.error("signalr connection error:", err);
    });

  window.Promiser = {
    registerNewPromise<T>(id: string) {
      const promise_with_resolvers = Promise.withResolvers<T>();
      this.pending_promises.set(id, promise_with_resolvers as any);
      return promise_with_resolvers.promise;
    },
    resolvePromise(value, id) {
      const promise_with_resolvers = this.pending_promises.get(id);
      if(promise_with_resolvers) {
        promise_with_resolvers.resolve(value);
        this.pending_promises.delete(id);
      }
    },
    rejectPromise(reason, id) {
      const promise_with_resolvers = this.pending_promises.get(id);
      if(promise_with_resolvers) {
        promise_with_resolvers.reject(reason);
        this.pending_promises.delete(id);
      }
    },
    pending_promises: new Map<string, ReturnType<typeof Promise.withResolvers<any>>>()
  } as IPromiser & {pending_promises: Map<string, ReturnType<typeof Promise.withResolvers>>};

  window.AsyncAndroidBridge = { 
    async requestMessageHistory(GetMessagesRequest_b64) {
      const id = crypto.randomUUID();
      const promise = Promiser.registerNewPromise<string>(id);
      (AndroidBridge as any).requestMessageHistory(GetMessagesRequest_b64, id);
      return WebviewControllerDecoder.decodeChatMessageHistoryRequestResponse(await promise);
    },
    async requestUserGroups() {
      const id = crypto.randomUUID();
      const promise = Promiser.registerNewPromise<string>(id);
      (AndroidBridge as any).requestUserGroups(id);
      return WebviewControllerDecoder.decodeGetUserGroupsResponse(await promise);
    }
  }
  
  AndroidBridge.setLoaded();
})



