import "./AddRoomElement"

import { css, html, LitElement } from "lit"
import { customElement, query } from "lit/decorators.js";
import { AddRoomElement } from "./AddRoomElement";

// Represents 'side menu' that contains a list of rooms and the ability to create a new chat room
@customElement("side-menu")
export class SideMenuElment extends LitElement {

    protected override render() { 
        return html`
            <div id="menu_popover_header" class="header">
                <div class="spacer"></div>
                <span class="header-text">Rooms</span>
                <button class="spacer" popovertarget="add_room_popover">
                    <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill="#242424"><path d="M0 0h24v24H0z" fill="none"/><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                </button>
                <add-room id="add_room_popover" popover="manual" @toggle="${this.onAddRoomToggled}"></add-room>
            </div>
            <slot id="room_slot"></slot>
        `;
    }

    @query("#add_room_popover", true)
    private _add_room_popover!: AddRoomElement;

    // Adds a listener that closes the AddRoomElement popover when a click happens outside of it (to patch behavior of multiple built-in popovers)
    private onAddRoomToggled(event: ToggleEvent) {
        event.newState === "open" ? this.addEventListener("click", this.onClickedWhileAddRoomOpen) : this.removeEventListener("click", this.onClickedWhileAddRoomOpen);
    }

    // Listener used in onAddRoomToggled
    private onClickedWhileAddRoomOpen = (event: MouseEvent) => {
        if(!(event.composedPath().includes(this._add_room_popover))) {
            this._add_room_popover.hidePopover();
        }
    }

    static styles = css`
        :host {
            position: absolute;
            width: 80%;
            height: 100%;
            top: 0;
            left: 0;
            z-index: 3;
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            background: #bfbfca;
        }

        :host([no-rooms]) {
            width: 100%;
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

        #add_room_popover { 
            position: absolute;
            flex-direction: column;
        }

        #add_room_popover:popover-open {
            display: flex;
        }
    `;
}