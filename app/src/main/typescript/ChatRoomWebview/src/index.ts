import "./Components/WebviewControllerElement"
import { ChatMessage, ChatRoom, MessageFns } from "./Generated/chat";
import { IWebviewController, IWebviewControllerDecoder } from "./Interfaces/IWebviewController";
import "./base.css";

declare global {
  const DEBUG: boolean;
  const AndroidBridge: IAndroidBridge;
  const WebviewController: IWebviewController;
  const WebviewControllerDecoder: IWebviewControllerDecoder;
  const DLOG: (val: any) => void;

  interface IAndroidBridge {
    getUserName(): string;
    getUserId(): string;

    setLoaded(): void;
    postMessage(b64: string): void;
  }

  interface Window {
    DEBUG: boolean;
    DLOG: (val: any) => void
    AndroidBridge: IAndroidBridge;
    WebviewController: IWebviewController;
    WebviewControllerDecoder: IWebviewControllerDecoder;
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

  // window.AndroidBridge = {
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
  // }

  window.DLOG = (val) => console.debug(val);

  const controller = document.querySelector('webview-controller');
  controller!.addRoom({id: "0", roomName: "Test Room"});
  //controller!.switchToRoom("0");
  controller!.addMessage(makeDummyMessage("0", "0", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));
  setTimeout(() => {
    controller!.addMessage(makeDummyMessage("1", "0", "2", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "User"));
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
  }
  AndroidBridge.setLoaded();
})



