import { css, CSSResultGroup, html, LitElement, PropertyValues } from "lit";
import type { IChatMessage, IChatMessageRoomUnaware } from "../Interfaces/IChatMessage";
import { customElement, property, state } from "lit/decorators.js";

// Simple element that represents a ChatMessage
@customElement("chat-message")
export class ChatMessageElement extends LitElement {
    @property({attribute: "user-id", reflect: true})
    accessor senderId!: string;
    
    @property({type: Number, attribute: "created-at", reflect: true})
    accessor createdAt!: number;

    @property({attribute: 'user-name', reflect: true})
    accessor senderName!: string;

    toIChatMessage(): IChatMessageRoomUnaware {
        return {
            id: this.id,
            content: this.innerText,
            createdAt: this.createdAt,
            userId: this.senderId,
            userName: this.senderName,
        }
    }

    protected override render() {
        return html`
            <div id="header">
                <div id="name">${this.senderName}</div>
                <div id="gap"></div>
                <div id="times">
                    <div id="created">${new Date(this.createdAt).toLocaleString()}</div>
                </div>
            </div>
            <div id="content"><slot id="slot_content"></slot></div>
        `;
    }

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            max-width: 80%;
            width: fit-content;
            box-sizing: border-box;
        }

        /* Header section stays on top of content */
        #header {
            display: flex;
            flex-direction: row;
            align-items: center;
            width: 100%;
            box-sizing: border-box;
            margin-bottom: 4px;
        }

        /* Name stays pinned to left */
        #name {
            flex-shrink: 0;
        }

        
        #gap {
            min-width: 5ch;
            flex: 1 1 auto;
        }

        /* Times block moves to the right */
        #times {
            display: flex;
            flex-direction: row;
            gap: 0.5rem;
            flex-shrink: 0;
        }

        /* Content expands */
        #content {
            display: inline-block;     /* makes width fit-content work correctly */
            padding: 0.5rem;           /* your bubble padding */
            border-radius: 8px;
            background: var(--msg-content-bg, transparent);
            box-sizing: border-box;

            /* width rules */
            width: fit-content;        /* expand to content width */
            max-width: 100%;           /* let :host enforce the 80% limit */
            min-width: calc(5ch);      /* “5 characters” extra min width */
            white-space: pre-wrap;     /* wrap long text */
            overflow-wrap: break-word;
            word-break: break-word;     /* break long words */
        }

        :host(.user) #content {
            background: var(--user-msg-content-bg, transparent);
        }
    `;
}

declare global {
  interface HTMLElementTagNameMap {
    "chat-message": ChatMessageElement;
  }
}