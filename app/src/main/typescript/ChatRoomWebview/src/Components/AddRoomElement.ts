import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, query, state } from "lit/decorators.js";
import { ISearchResult, ISearchResultItem } from "../Interfaces/ISearchResult";

@customElement("add-room")
export class AddRoomElement extends LitElement {
    override connectedCallback(): void { 
        super.connectedCallback();
        this.addEventListener("toggle", this.onHostToggled);
    }

    override disconnectedCallback(): void {
        super.disconnectedCallback();
        this.removeEventListener("toggle", this.onHostToggled);
    }

    protected override render() { 
       return html`
            <div class="add-room-container">
                <div class="header">
                    <span class="header-text">Create New Chat Room</span>
                </div>

                <div class="form-body">
                    <!-- Name row -->
                    <div class="field-row">
                        <label for="room_name_input">Name:</label>
                        <input
                            id="room_name_input"
                            class="text-input"
                            placeholder="Enter new room name..."
                            @change="${this.onNameChange}"/>
                    </div>

                    <!-- Users row -->
                    <div class="field-row">
                        <label for="user_search_input">Users:</label>
                        <div class="input-with-icon">
                        <input
                            id="user_search_input"
                            class="text-input"
                            placeholder="Search for users to add..."
                            @change="${this.onSearchChange}"/>
                        <svg id="search_icon" xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 0 24 24" width="24px" fill="#242424"><path d="M0 0h24v24H0z" fill="none" /><path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg>
                        </div>
                    </div>

                    <!-- Scrollable user bubble list -->
                    <div class="user-list-container">
                        <!-- Example bubbles; you’ll generate these from data -->
                        <!-- <div class="user-bubble">
                        <span class="user-name">alice</span>
                        <button class="user-remove-btn" aria-label="Remove alice">×</button>
                        </div>
                        <div class="user-bubble">
                        <span class="user-name">bob</span>
                        <button class="user-remove-btn" aria-label="Remove bob">×</button>
                        </div> -->
                        <!-- ... more bubbles here ... -->
                         ${this._selected_users.map(user => html`
                            <div class="user-bubble">
                                <span class="user-name">${user.displayName}</span>
                                <button class="user-remove-btn" for-id=${user.id} @click="${this.onRemoveSelectedUser}">X</button>
                            </div>
                         `)}

                    </div>

                    <!-- Buttons -->
                    <div class="buttons-row">
                        <button class="cancel-btn" @click="${this.onCancel}">Cancel</button>
                        <button class="ok-btn" ?disabled=${!this.submittable}>OK</button>
                    </div>

                    <div id="user_search_popover" popover="manual" @toggle="${this.onSearchPopoverToggled}">
                        ${this._current_search_result_items.map((user) => html`
                                <label class="search-result-item">
                                    <div class="search-result-text">
                                        <div class="search-result-display-name"> ${user.displayName}</div>
                                        <div class="search-result-email">${user.email}</div>
                                    </div>
                                    <input
                                        type="checkbox"
                                        class="search-result-checkbox"
                                        @change=${(e: Event) => this.onUserSearchCheckboxChanged(user, (e.currentTarget as HTMLInputElement).checked)}
                                    />
                                </label>
                            `
                        )}
                    </div>
                </div>
            </div>
        `;

    }

    private onCancel() { this.hidePopover(); }

    //set submittable if names non empty and this isnt empty
    private onNameChange(event: InputEvent) {
        const input = event.currentTarget as HTMLInputElement;
        this.submittable = input.value.length > 0 && this._selected_users.length > 0;
    }

    private onHostToggled = (event: ToggleEvent) => {
        if(event.newState === "closed") { 
            clearTimeout(this.onSearchChange.timeout);
            this._name_input.value = "";
            this._search_input.value = "";
            this._selected_users = [];
            this._current_search_result_items = [];
            this.submittable = false;
            this.onSearchChange.result?.then(() => {
                this._current_search_result_items = [];
                this.onSearchChange.result = undefined;
            });
        }
    }

    private onSearchPopoverToggled(event: ToggleEvent) { 
        event.newState === "open" ? this.addEventListener("click", this.onHostClickedWhileSearchPopoverOpen) : this.removeEventListener("click", this.onHostClickedWhileSearchPopoverOpen);
    }

    private onHostClickedWhileSearchPopoverOpen(event: MouseEvent) {
        if(!(event.composedPath().includes(this._search_popover))) {
            this._search_popover.hidePopover();
        }
    }

    private readonly onSearchChange = {
        timeout: undefined as ReturnType<typeof setTimeout> | undefined,
        result: undefined as Promise<ISearchResult> | undefined,
        handleEvent: (event: InputEvent) => {
            clearTimeout(this.onSearchChange.timeout);
            this.removeAttribute("searching");
            this.onSearchChange.result = undefined;

            this.onSearchChange.timeout = setTimeout(() => {
                this.setAttribute("searching", "");
                const promise = window.AsyncAndroidBridge.searchUsers((event.currentTarget as HTMLInputElement).value);
                promise.then(result => {
                    if(promise !== this.onSearchChange.result) return; // outdated result
                    this._current_search_result_items = result.results;
                    this.removeAttribute("searching");
                    this._search_popover.showPopover();
                });
                this.onSearchChange.result = promise;
            }, 300);
        }
    }

    private onUserSearchCheckboxChanged(user: ISearchResultItem, checked: boolean) {
        if(checked) {
            if(!this._selected_users.find(u => u.id === user.id)) {
                this._selected_users.push(user);
                this.requestUpdate("_selected_users");
                this.submittable = this._name_input.value.length > 0 && this._selected_users.length > 0;
                DLOG(`[AddRoomElement] Added user with id '${user.id}' to selected users.`);
            }
        }
        else {
            const index = this._selected_users.findIndex(u => u.id === user.id);
            if(index !== -1) {
                this._selected_users.splice(index, 1);
                this.requestUpdate("_selected_users");
                this.submittable = this._name_input.value.length > 0 && this._selected_users.length > 0;
                DLOG(`[AddRoomElement] Removed user with id '${user.id}' from selected users.`);
            }
        }
    }

    private onRemoveSelectedUser(event: MouseEvent) { 
        const button = event.currentTarget as HTMLButtonElement;
        const userId = button.getAttribute("for-id");
        const index = this._selected_users.findIndex(user => user.id === userId);
        if(index !== -1) {
            this._selected_users.splice(index, 1);
            this.requestUpdate("_selected_users");
            this.submittable = this._name_input.value.length > 0 && this._selected_users.length > 0;
            DLOG(`[AddRoomElement] Removed user with id '${userId}' from selected users.`);
        }
        else DLOG(`[AddRoomElement] Could not find user with id '${userId}' to remove from selected users.`);
    }

    @query("#room_name_input", true)
    private _name_input!: HTMLInputElement;

    @query("#user_search_input", true)
    private _search_input!: HTMLInputElement;

    @query("#user_search_popover", true)
    private _search_popover!: HTMLDivElement;

    @state()
    private submittable = false;

    @state()
    private _selected_users: ISearchResultItem[] = [];

    @state()
    private _current_search_result_items: ISearchResultItem[] = [];

    static styles = css`
        .search-result-item {
            display: flex;
            align-items: center;
            gap: 8px;

            padding: 4px 8px;
            width: 100%;
            box-sizing: border-box;

            cursor: pointer;           /* was none, but this is interactive */
        }

        /* Left side: text block */
        .search-result-text {
            display: flex;
            flex-direction: column;
            flex: 1 1 auto;
            min-width: 0;              /* ⭐ REQUIRED for ellipsis in flex layouts */
        }

        /* First line: display name */
        .search-result-display-name {
            font-size: 14px;
            line-height: 1.2;

            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        /* Second line: email, smaller and slightly muted */
        .search-result-email {
            font-size: 12px;
            line-height: 1.2;
            opacity: 0.8;

            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        /* Right side: checkbox, no stretching */
        .search-result-checkbox {
            flex: 0 0 auto;
            margin: 0;
        }

        .add-room-container {
            display: flex;
            flex-direction: column;
            width: 100%;
            height: 100%;
            box-sizing: border-box;
        }

        /* Header */
        .header {
            text-align: center;
            padding: 8px;
            background: #fff;
            box-shadow: 0 1px 4px rgba(0,0,0,0.15);
        }

        .header-text {
            font-weight: 600;
        }

        /* Form body */
        .form-body {
            display: flex;
            flex-direction: column;
            gap: 12px;
            padding: 12px 8px;
            flex: 1 1 auto;
            box-sizing: border-box;
        }

        /* Label + input row */
        .field-row {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        /* Left-aligned fixed-width labels */
        .field-row > label {
            flex: 0 0 75px;
            text-align: left;
            font-size: 14px;
        }

        /* Shared input styling */
        .text-input {
            flex: 1 1 auto;
            box-sizing: border-box;
            padding: 6px 8px;
            border: 1px solid #ccc;
            border-radius: 4px;

            font-size: 14px;
            line-height: 1.2;
            background: #fff;
        }

        /* Input with embedded icon */
        .input-with-icon {
            position: relative;
            flex: 1 1 auto;
            display: flex;
            align-items: center;
        }

        .input-with-icon > .text-input {
            padding-right: 32px; /* room for the icon */
        }

        @keyframes spin {
            from { transform: translateY(-50%) rotate(0deg); }
            to   { transform: translateY(-50%) rotate(360deg); }
        }

        #search_icon {
            position: absolute;
            right: 8px;
            top: 50%;
            transform: translateY(-50%);

            width: 20px;
            height: 20px;

            visibility: hidden;       /* toggle via JS */
            pointer-events: none;     /* decorative */
        }

        :host([searching]) #search_icon {
            visibility: visible;
            animation: spin 1s linear infinite;
        }

        #user_search_input {
            anchor-name: --user-search-input;
        }

        #user_search_popover { 
            /* kill UA default centering */
            margin: 0;
            inset: auto;

            position: absolute; /* or absolute if you prefer */
            position-anchor: --user-search-input;

            /* Put it below the input, aligned to its left edge */
            inset-block-start: anchor(bottom);
            inset-inline-start: anchor(start);
            inset-inline-end: anchor(end);

            /* or, simpler: position-area: block-end; if you want it directly under */
            /* position-area: block-end; */

            background: #fff;
            border: 1px solid #ccc;
            border-radius: 4px;
            padding: 4px;
        }

        /* Scrollable bubble list */
        .user-list-container {
            width: 100%;
            box-sizing: border-box;

            display: flex;
            flex-wrap: wrap;
            gap: 4px;

            padding: 0;
            border: none;
            background: none;

            /* Grows up to ~5–6 rows then scrolls */
            max-height: 7.5rem;
            overflow-y: auto;
            overflow-x: hidden;
        }

        /* Individual user "chips" */
        .user-bubble {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 2px 8px;

            border-radius: 999px;
            background: #e0e7ff;

            font-size: 14px;
            line-height: 1.2;

            white-space: nowrap;
        }

        .user-name {
            font-size: 14px;
        }

        .user-remove-btn {
            border: none;
            background: transparent;
            cursor: pointer;
            padding: 0;

            font-size: 14px;
            line-height: 1;
        }

        /* Buttons (bottom-right) */
        .buttons-row {
            margin-top: auto;
            display: flex;
            justify-content: flex-end;  
            gap: 8px;
        }

        .cancel-btn,
        .ok-btn {
            padding: 6px 12px;
            font-size: 14px;
        }
    `;
}