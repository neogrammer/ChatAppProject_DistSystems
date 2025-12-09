import { css, html, LitElement } from "lit";
import { customElement } from "lit/decorators.js";

@customElement("add-room")
export class AddRoomElement extends LitElement {
    override render() { 
        return html`
            <div class="header">
                <div class="spacer"></div>
                <span class="header-text">Create New Chat Room</span>
                <div class="spacer"></div>
            </div>
            <div>
                <label for="add_room_popover_room_name_input">Name: </label>
                <input id="add_room_popover_room_name_input" placeholder="Enter new room name..." />
            </div>
            <div>
                <label for="add_room_popover_user_search_input">Users: </label>
                <input id="add_room_popover_user_search_input" placeholder="Search for users to add..." />
                <svg id="add_room_popover_searching" xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill="#242424"><path d="M0 0h24v24H0z" fill="none"/><path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
            </div>
            <div>
                <button>Cancel</button>
                <button disabled>OK</button>
            </div>
        `;
    }

    static styles = css`
        #add_room_popover_searching {
            visibility: hidden;
        }

        .spacer {
            width: 36px;
            height: 36px;
        }

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

        .header-text {
            font-weight: 600;
            text-align: center;
        }

        .header > button, .icon {
            background: inherit;
            border: none;
        }
    `;
}