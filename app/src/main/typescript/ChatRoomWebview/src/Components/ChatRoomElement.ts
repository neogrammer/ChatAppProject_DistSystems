import "./ChatMessageElement"

import { css, CSSResultGroup, html, LitElement, PropertyValues } from "lit";
import { customElement, property, queryAssignedElements } from "lit/decorators.js";
import { ChatMessageElement } from "./ChatMessageElement";
import { IChatMessage, IChatMessageRoomUnaware } from "../Interfaces/IChatMessage";
import { ChatMessage } from "../Generated/chat";

// dont forget to set id
@customElement("chat-room")
export class ChatRoomElement extends LitElement {
    name!: string;
    roomId!: string;

    addMessage(message: ChatMessage): boolean {
        const insert_ref = this.findInsertionElement(message.createdAt);
        const insert_element = ChatRoomElement.protoToElement(message);
        insert_ref === null ? this.prepend(insert_element) : insert_ref.after(insert_element);
        if(message.userId === window.AndroidBridge.getUserId()) insert_element.classList.add('user');
        return true;
    }

    removeMessage(id: string): boolean {
        const elem = this.querySelector(`chat-message#${id}`);
        elem?.remove();
        return !!elem;
    }

    hasMessage(id: string): boolean {
        return !!this.querySelector(`chat-message#${id}`);
    }

    getMessage(id: string): IChatMessageRoomUnaware | null {
        const elem = this.querySelector(`chat-message#${id}`) as ChatMessageElement;
        if(!elem) return null;
        return elem.toIChatMessage();
    }

    override connectedCallback(): void {
        super.connectedCallback();
        this.addEventListener("scroll", this._onScroll);
    }

    override disconnectedCallback(): void {
        super.disconnectedCallback();
        this.removeEventListener("scroll", this._onScroll);
    }

    //todo 
    protected override firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        // show loading dialog

        // subscribe to message stream

        // get history

        // after subscription started and history fetched, prune duplicates

        // scroll to bottom on first render

        // hide loading dialog after its all done
    }

    protected override render() {
        return html`
            <slot id="slot_message" @slotchange="${this._onSlotChange}"></slot>
        `;
    }

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            align-items: flex-start;   /* right-justify slotted children */
            width: 100%;
            box-sizing: border-box;
            padding: 8px 8px;
        }

        slot::slotted(*) {
            display: block;
            margin: 0.25rem 0;
        }

        slot::slotted(.user) {
            align-self: flex-end;  /* left-justify user messages */
        }
    `;

    private readonly _onScroll = () => {
        const bottom = this.scrollHeight - this.clientHeight;
        const distance = bottom - this.scrollTop;

        this._should_pin_scroll = distance < 8; // wiggle margin
    };

    private _onSlotChange() {
        requestAnimationFrame(() => {
            if(this._should_pin_scroll) this.scrollTop = this.scrollHeight;
        })
    }

    private _should_pin_scroll = true;

    private findInsertionElement(insert_timestamp: number, messages = [...this.children] as ChatMessageElement[]): ChatMessageElement | null {
        let left = 0;
        let right = messages.length - 1;
        let result: ChatMessageElement | null = null;

        while (left <= right) {
            const mid = (left + right) >> 1;
            const midTime = messages[mid].createdAt;

            if (midTime < insert_timestamp) {
                result = messages[mid];
                left = mid + 1;    // try to find something later that's still < insert_timestamp
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    private static protoToElement(message: ChatMessage): ChatMessageElement {
        const element = document.createElement('chat-message');
        element.id = message.id;
        element.senderId = message.userId;
        element.senderName = message.userName;
        element.createdAt = message.createdAt;
        element.append(message.content);
        return element;
    }
}

declare global {
  interface HTMLElementTagNameMap {
    "chat-room": ChatRoomElement;
  }
}

// list of IProtobufChatMessage, transform to element
// appendChild ChatMessageElements (leaning more towards this)