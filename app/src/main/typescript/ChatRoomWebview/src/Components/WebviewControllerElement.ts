// use array of rooms + state index
import "./ChatRoomElement"
import { css, html, LitElement } from "lit";
import { IWebviewController } from "../Interfaces/IWebviewController";
import { IChatMessage } from "../Interfaces/IChatMessage";
import { IChatRoom } from "../Interfaces/IChatRoom";
import { ChatRoomElement } from "./ChatRoomElement";
import { customElement, query, state } from "lit/decorators.js";
import { ChatMessage } from "../Generated/chat";
import { randomUUID } from "crypto";

@customElement("webview-controller")
export class WebviewControllerElement extends LitElement implements IWebviewController {
    addRoom(room: IChatRoom): boolean {
        if(!room) {
            DLOG("[WebviewControllerElement] Failed to add room because argument was nullish!");
            return false;
        }
        if(!room.id || room.id.length === 0) {
            DLOG("[WebviewControllerElement] Failed to add room because argument id was null or 0 length!");
            return false;
        }
        if(!room.roomName || room.roomName.length === 0) {
            DLOG("[WebviewControllerElement] Failed to add room because argument roomName was null or 0 length!");
            return false;
        }
        if(this._rooms.has(room.id)) return false;
        const room_element = document.createElement('chat-room');
        room_element.name = room.roomName;
        this._rooms.set(room.id, room_element);
        return true;
    }

    removeRoom(roomId: string): boolean {
        if(!roomId || roomId.length === 0) {
            DLOG("[WebviewControllerElement] Failed to remove room because argument was null or 0 length!");
            return false;
        }
        if(roomId === this.current_room) {
            if(this._rooms.size === 1) {
                DLOG("[WebviewControllerElement] Failed to remove room because doing so would leave the app roomless!");
                return false;
            }
            const as_array = [...this._rooms];
            let new_index = [...this._rooms].findIndex(v => v[0] === roomId);
            if(new_index === 0) ++new_index;
            else --new_index;
            this.current_room = as_array[new_index][0];
        }

        return this._rooms.delete(roomId);
    }

    switchToRoom(roomId: string): boolean {
        if(!roomId || roomId.length === 0) {
            DLOG("[WebviewControllerElement] Failed to switch to room because argument was null or 0 length!");
            return false;
        }
        if(this._rooms.has(roomId)) {
            this.current_room = roomId;
            return true;
        }
        DLOG(`[WebviewControllerElement] Failed to switch to room because roomId '${roomId}' doesn't exist!`);
        return false;
    }

    addMessage(message: ChatMessage): boolean {
        if(!message) {
            DLOG("[WebviewControllerElement] Failed to add message because argument was nullish!");
            return false;
        }
        const room_id = message.roomId;
        const room = this._rooms.get(room_id);
        if(!room) {
            DLOG(`[WebviewControllerElement] Failed to add message because room id couldn't be found! Message: ${message}`);
            return false;
        }
        return room.addMessage(message);
    }

    addMessages(...messages: ChatMessage[]): boolean {
        if(!messages || messages.length === 0) {
            DLOG("[WebviewControllerElement] Failed to add messages because argument was nullish or array was empty!");
            return false;
        }
        let all_true = true;
        for(const message of messages) {
            const result = this.addMessage(message);
            all_true &&= result;
        }
        return all_true;
    }

    removeMessage(messageId: string, roomId: string): boolean {
        if(!messageId || messageId.length === 0) {
            DLOG(`[WebviewControllerElement] Failed to remove message because messageId argument was nullish or empty! roomId: ${roomId}`)
            return false;
        }
        if(!roomId || roomId.length === 0) {
            DLOG(`[WebviewControllerElement] Failed to remove message because roomId argument was nullish or empty! messageId: ${messageId}`)
            return false;
        }
        const room = this._rooms.get(roomId);
        if(!room) {
            DLOG(`[WebviewControllerElement] Failed to remove message because roomId '${roomId}' doesn't exist! messageId: ${messageId}`)
            return false;
        }
        return room.removeMessage(messageId);
    }

    removeMessages(...messageIds: [messageId: string, roomId: string][]): boolean {
        if(!messageIds || messageIds.length === 0) {
            DLOG("[WebviewControllerElement] Failed to remove messages because argument array was null or empty!");
            return false;
        }
        let all_true = true;
        for(const message of messageIds) {
            const result = this.removeMessage(...message);
            all_true &&= result;
        }
        return all_true;
    }

    hasMessage(messageId: string, roomId?: string): boolean {
        if(!messageId) {
            DLOG(`[WebviewControllerElement] Failed to check for message because messageId argument was nullish or empty! roomId: ${roomId}`)
            return false;
        }
        if(roomId && roomId.length !== 0) {
            const room = this._rooms.get(roomId);
            if(!room) {
                DLOG(`[WebviewControllerElement] Failed to remove message because room doesn't exist for roomId '${roomId}'! messageId: ${messageId}`)
                return false;
            }
            return room.hasMessage(messageId);
        }
        for(const room of this._rooms) {
            if(this.hasMessage(messageId, room[0])) return true;
        }
        return false;
    }

    getMessage(messageId: string, roomId?: string): ChatMessage | null {
        if(!messageId) {
            //todo log
            return null;
        }
        if(roomId && roomId.length !== 0) {
            const room = this._rooms.get(roomId);
            if(!room) {
                //todo log
                return null;
            }
            const msg = room.getMessage(messageId);
            if(msg === null) {
                //todo log
                return null;
            }
            msg.roomId = roomId;
            msg.roomName = room.name;
            return msg as unknown as ChatMessage;
        }
        for(const room of this._rooms) {
            const msg = this.getMessage(messageId, room[0]);
            if(msg !== null) return msg;
        }
        return null;
    }

    asElement<T extends Element = Element>(): T {
        return this as unknown as T;
    }

    @state()
    private current_room = "";

    @query("#msg_input", true)
    private _input!: HTMLInputElement;

    private _onSend() {
        if(!this._input.value || this._input.value.length === 0) return;
        const message: ChatMessage = {
            content: this._input.value,
            createdAt: Date.now(),
            roomId: this.current_room,
            userId: window.AndroidBridge.getUserId(),
            userName: window.AndroidBridge.getUserName(),
            id: crypto.randomUUID()
        }
        
        const room = this._rooms.get(this.current_room);

        if(!room) {
            DLOG(`[WebviewControllerElement] Failed to send message because roomId '${this.current_room}' no longer exists!`);
        }

        if(!room!.addMessage(message)) return;

        const encoded = ChatMessage.encode(message).finish();
        window.AndroidBridge.postMessage(btoa(String.fromCharCode(...encoded)));
    }

    protected override render() {
        if(this.current_room.length === 0) return html``;
        const room = this._rooms.get(this.current_room);
        if(!room) return html``;

        return html`
            <div id="header">
                <span id="room_name">${room.name}</span>
            </div>
            ${room}
            <div id="footer">
                <input id="msg_input" placeholder="Send message..."/>
                <svg @click="${this._onSend}" id="send_btn" xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill="#789DE5"><path d="M0 0h24v24H0z" fill="none"/><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
            </div>
        `
    }

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            width: 100vw;
            height: 100vh;
            box-sizing: border-box;
            overflow: hidden;
            background: #f5f5f7; /* Slightly off-white background for the whole app */
        }

        /* HEADER — fixed at top */
        #header {
            padding: 8px;
            box-sizing: border-box;
            flex: 0 0 auto;
            background: #ffffff;
            box-shadow: 0 1px 4px rgba(0,0,0,0.15); /* subtle elevation */
            display: flex;
            justify-content: center;  /* center horizontally */
            align-items: center;      /* center vertically */
            font-weight: 600;
            z-index: 2; /* keeps shadow crisp above chat-room scroll area */
        }

        /* CHAT-ROOM — fills remaining vertical space */
        chat-room {
            flex: 1 1 auto;
            min-height: 0;
            overflow-y: auto;
            width: 100%;
            box-sizing: border-box;
            background: #ececf1;  /* soft grey chat background */
        }

        /* FOOTER — fixed at bottom */
        #footer {
            padding: 8px;
            box-sizing: border-box;
            flex: 0 0 auto;
            background: #ffffff;
            box-shadow: 0 -1px 4px rgba(0,0,0,0.15); /* elevation from above */
            display: flex;
            align-items: center;
            gap: 8px;
            z-index: 2;
        }

        /* INPUT expands as much as possible */
        #msg_input {
            flex: 1 1 auto;   /* grow into available space */
            padding: 6px 8px;
            border: 1px solid #ccc;
            border-radius: 6px;
            font-size: 16px;
            background: #fafafa;
            box-sizing: border-box;
        }

        /* SEND BUTTON — consistent size + elevation feedback */
        #send_btn {
            flex: 0 0 auto;
            cursor: pointer;
            border-radius: 4px;
            padding: 4px; /* clickable area */
            transition: background 0.15s ease;
        }

        /* Hover state */
        #send_btn:hover {
            background: rgba(0,0,0,0.07);
        }

        /* Active/pressed state */
        #send_btn:active {
            background: rgba(0,0,0,0.15);
        }


    `;

    private _rooms: Map<string, ChatRoomElement> = new Map();

}

declare global {
  interface HTMLElementTagNameMap {
    "webview-controller": WebviewControllerElement;
  }
}
