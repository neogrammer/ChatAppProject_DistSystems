// use array of rooms + state index
import "./ChatRoomElement"
import "./SideMenuElement"
import "../init"
import { css, html, LitElement, PropertyValues } from "lit";
import { IWebviewController } from "../Interfaces/IWebviewController";
import { ChatRoomElement } from "./ChatRoomElement";
import { customElement, query, state } from "lit/decorators.js";
import { map } from "lit/directives/map.js";
import { ChatMessage, GroupInfo } from "../Generated/chat";
import { ensureInitialized } from "../init";
import { SideMenuElment } from "./SideMenuElement";

// App host that implements IWebviewController
@customElement("webview-controller")
export class WebviewControllerElement extends LitElement implements IWebviewController {
    constructor() {
        super();
        window.WebviewController = this;
        ensureInitialized();
    }

    /**
     * Registers a new room and switches to it if it is the first one.
     * Returns false for invalid metadata or duplicate ids.
     */
    addRoom(room: GroupInfo): boolean {
        if(!room) {
            DLOG("[WebviewControllerElement] Failed to add room because argument was nullish!");
            return false;
        }
        if(!room.id || room.id.length === 0) {
            DLOG("[WebviewControllerElement] Failed to add room because argument id was null or 0 length!");
            return false;
        }
        if(!room.groupName || room.groupName.length === 0) {
            DLOG("[WebviewControllerElement] Failed to add room because argument roomName was null or 0 length!");
            return false;
        }
        if(this._rooms.has(room.id)) return false;
        const room_element = document.createElement('chat-room');
        room_element.name = room.groupName;
        room_element.roomId = room.id;
        this._rooms.set(room.id, room_element);
        this.requestUpdate("_rooms");
        if(this._rooms.size === 1) this.switchToRoom(room.id); //todo maybe move to end of firstUpdated
        return true;
    }

    // Removes a room from the UI. If its the last room, the app shows the side menu fullscreen. If the room to remove is the current room, it switches to the next room.
    removeRoom(roomId: string): boolean {
        if(!roomId || roomId.length === 0) {
            DLOG("[WebviewControllerElement] Failed to remove room because argument was null or 0 length!");
            return false;
        }
        if(roomId === this.current_room) {
            if(this._rooms.size === 1) {
                this._popover.showPopover();
                this.current_room = "";
                this._rooms.clear();
                this.requestUpdate("_rooms");
                DLOG("[WebviewControllerElement] Removed last room, no rooms left! Switching to empty room view.");
                return true;
            }
            const as_array = [...this._rooms];
            let new_index = [...this._rooms].findIndex(v => v[0] === roomId);
            if(new_index === 0) ++new_index;
            else --new_index;
            this.current_room = as_array[new_index][0];
            DLOG("[WebviewControllerElement] Removed current room, switching to room " + this.current_room + ` (${as_array[new_index][1].name})`);
        }

        const return_val = this._rooms.delete(roomId);
        if(return_val) this.requestUpdate("_rooms");
        return return_val;
    }

    // Switches to the room, and hides the side menu.
    switchToRoom(roomId: string): boolean {
        if(!roomId || roomId.length === 0) {
            DLOG("[WebviewControllerElement] Failed to switch to room because argument was null or 0 length!");
            return false;
        }
        if(roomId === this.current_room) return true;
        if(this._rooms.has(roomId)) {
            this.current_room = roomId;
            this.updateComplete.then(() => { this._popover.hidePopover(); });
            return true;
        }
        DLOG(`[WebviewControllerElement] Failed to switch to room because roomId '${roomId}' doesn't exist!`);
        return false;
    }

    // Routes a message to the appropriate chat room.
    addMessage(message: ChatMessage): boolean {
        if(!message) {
            DLOG("[WebviewControllerElement] Failed to add message because argument was nullish!");
            return false;
        }
        if(this._sentIds.has(message.id)) {
            DLOG("[WebviewControllerElement] Skipping duplicate user message!");
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

    // Routes multiple messages to the appropriate chat room
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

    // Removes a message from the chat room, if it exists. Purely visual; doesn't tell the server.
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

    // Removes many messages from the chat room, if it exists. Purely visual; doesn't tell the server.
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

    // Checks to see if a message exists. If a roomId is given, then it checks that particular room, otherwise, it checks all rooms.
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

    // Gets a message from the app and a room, if given.
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

    // On initialization, fetches the user's groups.
    protected override firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        DLOG("[WebviewControllerElement] Fetching user groups from AndroidBridge...");
        AndroidBridge.showLoadingDialog();
        AsyncAndroidBridge.requestUserGroups().then((groups) => {
            DLOG(`[WebviewControllerElement] Received ${groups.length} user groups from AndroidBridge.`);
            groups.forEach((group) => {
                this.addRoom(group);
            });
        }).catch((err) => {
            DLOG("[WebviewControllerElement] Failed to fetch user groups from AndroidBridge: " + err);
        }).finally(() => {
            if(this._rooms.size === 0) {
                DLOG("[WebviewControllerElement] No user groups found, showing side menu!");
                this.updateComplete.then(() => this._popover.showPopover());
            }
            AndroidBridge.hideLoadingDialog();
        });
    }

    @state()
    private current_room = "";

    @query("#msg_input", true)
    private accessor _input!: HTMLInputElement;

    @query("#menu_popover", true)
    private accessor _popover!: SideMenuElment;

    private _sentIds = new Set<string>();

    // Sends a message from the user to the chat room
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
            return;
        }

        if(!room!.addMessage(message)) return;
        this._sentIds.add(message.id);
        const encoded = ChatMessage.encode(message).finish();
        window.AndroidBridge.postMessage(btoa(String.fromCharCode(...encoded)));
        DLOG(`[WebviewControllerElement] Sent message ${message.content}`);
    }

    protected override render() {
        const room = this._rooms.get(this.current_room); //todo use when()

        return html`
            <div id="room_header" class="header">
                <button popovertarget="menu_popover" class="spacer">
                    <svg xmlns="http://www.w3.org/2000/svg" class="button" height="24px" viewBox="0 0 24 24" width="24px" fill="#242424"><path d="M0 0h24v24H0z" fill="none"/><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>
                </button>
                <span id="room_name" class="header-text">${room?.name || ""}</span>
                <div class="spacer"></div>
            </div>
            ${room}
            <div id="room_footer" class="input-with-icon-container">
                <input id="msg_input" placeholder="Send message..."/>
                <svg @click="${this._onSend}" class="button icon" id="send_btn" xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill="#789DE5"><path d="M0 0h24v24H0z" fill="none"/><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
            </div>
            <side-menu id="menu_popover" popover ?no-rooms="${this._rooms.size === 0}">
                ${map([...this._rooms], (room_pair) => html`<div class="button list-button ${room_pair[0] === this.current_room ? "active" : ""}" @click="${() => {this.switchToRoom(room_pair[0]);}}">${room_pair[1].name}</div>`)}
            </side-menu>
        `
    }

    static styles = css`
        :host {
            width: 100vw;
            height: 100vh;
        }

        .container, :host {
            display: flex;
            flex-direction: column;
            box-sizing: border-box;
            overflow: hidden;
            background: #f5f5f7;
        }

        /* HEADER — fixed at top */
        .header {
            position: relative;
            display: flex;
            align-items: center;
            justify-content: space-between;   /* center children horizontally */

            padding: 8px;
            box-sizing: border-box;
            background: #ffffff;
            box-shadow: 0 1px 4px rgba(0,0,0,0.15);
            z-index: 2;
        }

        /* Pin the button to the left edge */
        .header > button, .icon {
            background: inherit;
            border: none;
        }

        .spacer {
            width: 36px;
            height: 36px;
        }

        /* Centered title */
        .header-text {
            font-weight: 600;
            text-align: center;
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

        chat-room, #menu_popover {
            background: #ececf1;
        }

        /* FOOTER — fixed at bottom */
        .input-with-icon-container {
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
        .input-with-icon-container > input {
            flex: 1 1 auto;   /* grow into available space */
            padding: 6px 8px;
            border: 1px solid #ccc;
            border-radius: 6px;
            font-size: 16px;
            background: #fafafa;
            box-sizing: border-box;
        }

        /* SEND BUTTON — consistent size + elevation feedback */
        .input-with-icon-container > .icon {
            flex: 0 0 auto;
            cursor: pointer;
            border-radius: 4px;
            padding: 4px; /* clickable area */
        }

        .button {
            transition: background 0.15s ease;
        }

        .list-button { 
            width: 100%;
            padding-top: 2px;
            padding-bottom: 2px;
            box-sizing: border-box;
            text-align: center;
        }

        /* Hover state */
        .button:hover {
            background: rgba(0,0,0,0.07);
        }

        /* Active/pressed state */
        .button.active {
            background: rgba(0,0,0,0.15);
        }


    `;

    @state()
    private _rooms: Map<string, ChatRoomElement> = new Map();

}

declare global {
  interface HTMLElementTagNameMap {
    "webview-controller": WebviewControllerElement;
  }
}
