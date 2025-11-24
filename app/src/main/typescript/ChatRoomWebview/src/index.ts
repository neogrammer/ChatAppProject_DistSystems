import "./Components/WebviewControllerElement"
import { IProtobufChatMessage } from "./Interfaces/IProtobufChatMessage";
import "./base.css";



declare global {
  const DEBUG: boolean;
  const AndroidBridge: IAndroidBridge;

  interface IAndroidBridge {
    getUserName(): string;
    getUserId(): string;
  }

  interface Window {
    DEBUG: boolean;
    AndroidBridge: IAndroidBridge;
  }
}

function makeDummyMessage(id: string, roomId: string, userId: string, content: string, userName: string): IProtobufChatMessage {
  return {
    getId: () => id,
    getContent: () => content,
    getCreatedAt: () => Date.now(),
    getRoomId: () => roomId,
    getUserId: () => userId,
    getUserName: () => userName
  }
}

const controller = document.querySelector('webview-controller');
controller!.addRoom({id: "0", roomName: "Test Room"});
controller!.switchToRoom("0");
controller!.addMessage(makeDummyMessage("0", "0", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));
setTimeout(() => {
  controller!.addMessage(makeDummyMessage("1", "0", "2", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "User"));
}, 1000);
