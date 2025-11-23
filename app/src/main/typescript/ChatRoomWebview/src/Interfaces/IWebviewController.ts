import { IChatMessage } from "./IChatMessage";
import { IChatRoom } from "./IChatRoom";
import type { IElementConvertable } from "./IElementConvertable";
import { IProtobufChatMessage } from "./IProtobufChatMessage";

// Represents the interface between the wider app and the chat webview
export interface IWebviewController extends IElementConvertable {
    addRoom(room: IChatRoom): boolean;
    removeRoom(roomId: string): boolean;
    switchToRoom(roomId: string): boolean;

    addMessage(message: IProtobufChatMessage): boolean;
    addMessages(...messages: IProtobufChatMessage[]): boolean;
    removeMessage(messageId: string, roomId: string) : boolean;
    removeMessages(...messageIds: [messageId: string, roomId: string][]): boolean;
    hasMessage(messageId: string, roomId?: string): boolean;
    getMessage(messageId: string, roomId?: string) : IChatMessage | null;
}