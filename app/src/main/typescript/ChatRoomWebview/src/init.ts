import * as signalR from "@microsoft/signalr";
import { AddUserToGroupResponse, ChatMessage, CreateGroupResponse, GetMessagesRequest, GetMessagesResponse, GetUserGroupsRequest, GetUserGroupsResponse, GroupInfo, SearchUsersResponse } from "./Generated/chat";
import { WebviewControllerElement } from "./Components/WebviewControllerElement";
import { IPromiser } from "./Interfaces/IWebviewController";

let initialized = false;

/**
 * One-time initialization hook for the webview runtime. Sets up debugging,
 * protobuf encoders/decoders, SignalR connection handlers, and async Android bridge helpers.
 */
export function ensureInitialized() {
    if(initialized) return;
    initialized = true;

    // if statement for tree shaking
    if(DEBUG) {
        window.DLOG = (val) => console.debug(val);
    }
    else {
        window.DLOG = (val) => {}
    }

    /**
     * Protobuf decoders for Android bridge payloads.
     */
    window["WebviewControllerDecoder"] = {
        /**
         * Converts a base64 string into a Uint8Array for protobuf decoding.
         */
        toByteArray(b64: string) {
            const bin = atob(b64);
            const len = bin.length;
            const bytes = new Uint8Array(len);
            for (let i = 0; i < len; i++) {
                bytes[i] = bin.charCodeAt(i);
            }
            return bytes;
        },
        /**
         * Decodes a base64-encoded ChatMessage protobuf.
         */
        decodeChatMessage(b64: string) {
            return ChatMessage.decode(this.toByteArray(b64));
        },
        /**
         * Decodes a base64-encoded GroupInfo protobuf.
         */
        decodeChatRoom(b64) {
            return GroupInfo.decode(this.toByteArray(b64));
        },
        /**
         * Decodes a message history response and extracts messages.
         */
        decodeChatMessageHistoryRequestResponse(b64: string) { 
            return GetMessagesResponse.decode(this.toByteArray(b64)).messages;
        },
        /**
         * Decodes a get-user-groups response and extracts groups.
         */
        decodeGetUserGroupsResponse(b64) {
            return GetUserGroupsResponse.decode(this.toByteArray(b64)).groups;
        },
        /**
         * Decoder for search results.
         */
        decodeSearchResult(b64: string) {
            return SearchUsersResponse.decode(this.toByteArray(b64))
        },
        /**
         * Decoder for create group response
         */
        decodeCreateGroupResponse(b64) {
            return CreateGroupResponse.decode(this.toByteArray(b64));
        },
        /**
         * Decoder for add user to group response
         */
        decodeAddUserToGroupResponse(b64) {
            return AddUserToGroupResponse.decode(this.toByteArray(b64)).success;
        },
    }
    /**
     * Protobuf encoders for Android bridge payloads.
     */
    window["WebviewControllerEncoder"] = { 
        /**
         * Encodes a ChatMessage protobuf and returns it as base64.
         */
        encodeChatMessage(message: ChatMessage) {
            return btoa(String.fromCharCode(...ChatMessage.encode(message).finish()));
        },
        /**
         * Encodes a GroupInfo protobuf and returns it as base64.
         */
        encodeGroupInfo(room: GroupInfo) {
            return btoa(String.fromCharCode(...GroupInfo.encode(room).finish()));
        },
        /**
         * Encodes a GetMessagesRequest protobuf and returns it as base64.
         */
        encodeChatMessageHistoryRequest(request: GetMessagesRequest) {
            return btoa(String.fromCharCode(...GetMessagesRequest.encode(request).finish()));
        },
        /**
         * Encodes a GetUserGroupsRequest protobuf and returns it as base64.
         */
        encodeGetUserGroupsRequest(request: GetUserGroupsRequest) {
            return btoa(String.fromCharCode(...GetUserGroupsRequest.encode(request).finish()));
        }
    }

    const CHAT_SERVICE_URL = "http://10.0.2.2:5066";

    // setup signalr connection
    const userId = window.AndroidBridge.getUserId();
    window.connection = new signalR.HubConnectionBuilder()
        .withUrl(`${CHAT_SERVICE_URL}/chathub?userId=${userId}`)
        .withAutomaticReconnect()
        .build();

    // handle incoming messages
    connection.on("ReceiveMessage", (message: ChatMessage) => {
        DLOG("received message via signalr");
        DLOG(message);
        window.WebviewController.addMessage(message);
    });

    // handle group added notifications
    connection.on("GroupAdded", (groupId: string, groupName: string) => {
        DLOG("received GroupAdded notification via signalr");
        DLOG(`Group: ${groupName} (${groupId})`);
        window.WebviewController.addRoom({id: groupId, groupName: groupName});
    });

    /**
     * Promise registry used to bridge Android callbacks to async/await.
     */
    window.Promiser = {
        /**
         * Registers a new promise keyed by id and returns the promise.
         */
        registerNewPromise<T>(id: string) {
            const promise_with_resolvers = Promise.withResolvers<T>();
            this.pending_promises.set(id, promise_with_resolvers as any);
                return promise_with_resolvers.promise;
        },
        /**
         * Resolves a pending promise by id and cleans it up.
         */
        resolvePromise(value, id) {
            const promise_with_resolvers = this.pending_promises.get(id);
            if(promise_with_resolvers) {
                promise_with_resolvers.resolve(value);
                this.pending_promises.delete(id);
            }
        },
        /**
         * Rejects a pending promise by id and cleans it up.
         */
        rejectPromise(reason, id) {
            const promise_with_resolvers = this.pending_promises.get(id);
            if(promise_with_resolvers) {
                promise_with_resolvers.reject(reason);
                this.pending_promises.delete(id);
            }
        },
        pending_promises: new Map<string, ReturnType<typeof Promise.withResolvers<any>>>()
    } as IPromiser & {pending_promises: Map<string, ReturnType<typeof Promise.withResolvers>>};

    /**
     * Async wrappers around AndroidBridge that resolve via Promiser and decode protobuf payloads.
     */
    window.AsyncAndroidBridge = { 
        /**
         * Requests message history through Java and returns decoded messages.
         */
        async requestMessageHistory(GetMessagesRequest_b64) {
            const id = crypto.randomUUID();
            const promise = Promiser.registerNewPromise<string>(id);
            try {
                (AndroidBridge as any).requestMessageHistory(GetMessagesRequest_b64, id);
                return WebviewControllerDecoder.decodeChatMessageHistoryRequestResponse(await promise);
            } catch(ex) {
                DLOG("[AsyncAndroidBridge] Failed to get message history: " + ex);
                AndroidBridge.showErrorDialog("Failed to get message history", "Failed to get message history for the current room!", true);
                return [];
            }
        },
        /**
         * Requests user groups through Java and returns decoded groups.
         */
        async requestUserGroups() {
            const id = crypto.randomUUID();
            const promise = Promiser.registerNewPromise<string>(id);
            try {
                (AndroidBridge as any).requestUserGroups(id);
                return WebviewControllerDecoder.decodeGetUserGroupsResponse(await promise);
            } catch(ex) {
                DLOG("[AsyncAndroidBridge] Failed to get user groups: " + ex);
                AndroidBridge.showErrorDialog("Failed to get your chat rooms", "The app couldn't get your chat rooms, are you connected and are the servers up?", false);
                return [];
            }
        },
        /**
         * Searches users via Java; short-circuits empty queries.
         */
        async searchUsers(substring: string) {
            if(substring.length === 0) {
                return {users: []};
            }
            const id = crypto.randomUUID();
            const promise = Promiser.registerNewPromise<string>(id);
            try {
                (AndroidBridge as any).searchUsers(substring, id);
                return WebviewControllerDecoder.decodeSearchResult(await promise);
            } catch(ex) {
                DLOG(`[AsyncAndroidBridge] Failed to search users (substring = ${substring}): ${ex}`);
                AndroidBridge.showErrorDialog("Failed to search for users", "The app couldn't execute the search, are you connected and are the servers up?", true);
                return {users: []};
            }
        },
        /**
         * Creates a group via Java; short-circuits empty queries.
         */
        async createGroup(name: string) {
            if(name.length === 0) {
                return {success: false, groupId: ""}
            }
            const id = crypto.randomUUID();
            const promise = Promiser.registerNewPromise<string>(id);
            try {
                (AndroidBridge as any).createGroup(name, id);
                return WebviewControllerDecoder.decodeCreateGroupResponse(await promise);
            } catch(ex) {
                DLOG(`[AsyncAndroidBridge] Failed to create group with (name = ${name}): ${ex}`);
                AndroidBridge.showErrorDialog("Failed to add chat room", "The app couldn't add the chat room, are you connected and are the servers up?", true);
                return {groupId: "", success: false};
            }
            
        },

        /**
         * Adds users to a group via Java
         */
        async addUserToGroup(userId, groupId) {
            if(userId.length === 0 || groupId.length === 0) {
                return false;
            }
            const id = crypto.randomUUID();
            const promise = Promiser.registerNewPromise<string>(id);
            try {
                (AndroidBridge as any).addUserToGroup(userId, groupId, id);
                return WebviewControllerDecoder.decodeAddUserToGroupResponse(await promise);
            } catch(ex) {
                DLOG(`[AsyncAndroidBridge] Failed to add user to group with userId = ${userId} and group = ${groupId}: ${ex}`);
                return false;
            }
        },
    };


    (async () => {
        // connect
        await connection.start()
            .then(() => {
                DLOG("signalr connected");
            })
            .catch(err => {
                console.error("signalr connection error:", err);
                AndroidBridge.showErrorDialog("Failed to connect to message service", "The app couldn't connect to the message service, are you connected to the server?", false);
            });
        
        // wait for script to finish
        await customElements.whenDefined("webview-controller");

        // wait for all updates to finish
        while(!(await WebviewController.asElement<WebviewControllerElement>().updateComplete)) {}

        // let app make async requests
        AndroidBridge.setLoaded();
    })();
}