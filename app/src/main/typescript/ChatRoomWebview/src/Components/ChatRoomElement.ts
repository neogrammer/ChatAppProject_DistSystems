import "./ChatMessageElement"

import { css, CSSResultGroup, html, LitElement } from "lit";
import { customElement, property, queryAssignedElements } from "lit/decorators.js";
import { IProtobufChatMessage } from "../Interfaces/IProtobufChatMessage";
import { ChatMessageElement } from "./ChatMessageElement";
import { IChatMessage, IChatMessageRoomUnaware } from "../Interfaces/IChatMessage";

// dont forget to set id
@customElement("chat-room")
export class ChatRoomElement extends LitElement {
    name!: string;

    addMessage(message: IProtobufChatMessage): boolean {
        const insert_ref = this.findInsertionElement(message.getCreatedAt());
        const insert_element = ChatRoomElement.protoToElement(message);
        insert_ref === null ? this.prepend(insert_element) : insert_ref.after(insert_element);
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

    private static protoToElement(message: IProtobufChatMessage): ChatMessageElement {
        const element = document.createElement('chat-message');
        element.id = message.getId();
        element.senderId = message.getUserId();
        element.senderName = message.getUserName();
        element.createdAt = message.getCreatedAt();
        element.append(message.getContent());
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