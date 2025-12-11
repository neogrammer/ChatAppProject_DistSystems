import * as signalR from "@microsoft/signalr";
import { ChatMessage, CreateGroupResponse, GetMessagesRequest, GetMessagesResponse, GetUserGroupsRequest, GetUserGroupsResponse, GroupInfo } from "./Generated/chat";

let initialized = false;

/**
 * One-time initialization hook for the webview runtime. Sets up debugging,
 * protobuf encoders/decoders, SignalR connection handlers, and async Android bridge helpers.
 */
export function ensureInitialized() {
    if(initialized) return;
    initialized = true;
    if(DEBUG) {
        /**
         * Builds a synthetic ChatMessage for local debug rendering.
         */
        function makeDummyMessage(id: string, roomId: string, userId: string, content: string, userName: string): ChatMessage {
            return {
            id: id,
            content: content,
            createdAt: Date.now(),
            roomId: roomId,
            userId: userId,
            userName: userName
            }
        }

  // window.AndroidBridge ??= {
  //   getUserId() {
  //     return "0"
  //   },
  //   getUserName() {
  //     return "Tyler"
  //   },
  //   postMessage(b64) {
      
  //   },
  //   setLoaded() {
      
  //   },
  //   requestMessageHistory(ChatMessageHistoryRequest_b64) {
      
  //   },
  //   showLoadingDialog() {},
  //   hideLoadingDialog() {}
  // }

  // const controller = document.querySelector('webview-controller');
  // controller!.addRoom({id: "main", roomName: "Anonymous"});
  // controller!.addRoom({id: "second", roomName: "Second Room"});
  // //controller!.switchToRoom("0");
  // controller!.addMessage(makeDummyMessage("0", "main", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));
  // setTimeout(() => {
  //   controller!.addMessage(makeDummyMessage("1", "main", "2", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "User"));
  //   controller!.addMessage(makeDummyMessage("2", "main", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));
  //   controller!.addMessage(makeDummyMessage("3", "main", "0", "Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message Test message ", "Tyler"));

  // }, 1000);
  
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
         * Placeholder decoder for search results.
         */
        decodeSearchResult(b64: string) {
            return {results: []}; //todo
        },
        decodeCreateGroupResponse(b64) {
            return CreateGroupResponse.decode(this.toByteArray(b64));
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
    //const ROOM_ID = "main";

    // setup signalr connection
    window.connection = new signalR.HubConnectionBuilder()
        .withUrl(`${CHAT_SERVICE_URL}/chathub`)
        .withAutomaticReconnect()
        .build();

    // handle incoming messages
    connection.on("ReceiveMessage", (message: ChatMessage) => {
        DLOG("received message via signalr");
        DLOG(message);
        window.WebviewController.addMessage(message);
    });

    // connect and join room
    connection.start()
        .then(() => {
        DLOG("signalr connected");
        //return connection.invoke("JoinGroup", ROOM_ID);
        })
        // .then(() => {
        //   DLOG(`joined room: ${ROOM_ID}`);
        // })
        .catch(err => {
            console.error("signalr connection error:", err);
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
            (AndroidBridge as any).requestMessageHistory(GetMessagesRequest_b64, id);
            return WebviewControllerDecoder.decodeChatMessageHistoryRequestResponse(await promise);
        },
        /**
         * Requests user groups through Java and returns decoded groups.
         */
        async requestUserGroups() {
            const id = crypto.randomUUID();
            const promise = Promiser.registerNewPromise<string>(id);
            (AndroidBridge as any).requestUserGroups(id);
            return WebviewControllerDecoder.decodeGetUserGroupsResponse(await promise);
        },
        /**
         * Searches users via Java; short-circuits empty queries.
         */
        async searchUsers(substring: string) {
            if(substring.length === 0) {
                return {results: []};
            }
            const id = crypto.randomUUID();
            const promise = Promiser.registerNewPromise<string>(id);
            (AndroidBridge as any).searchUsers(substring, id);
            return WebviewControllerDecoder.decodeSearchResult(await promise);
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
            (AndroidBridge as any).createGroup(name, id);
            return WebviewControllerDecoder.decodeCreateGroupResponse(await promise);
        },
    }
    
    customElements.whenDefined("webview-controller").then(() => {AndroidBridge.setLoaded()});
}

/**
 * Provides a browser-only mock AndroidBridge for local debugging.
 */
function enableMockComponents() {
    window.AndroidBridge ??= {
        getUserId() {
            return "0"
        },
        getUserName() {
            return "Tyler"
        },
        postMessage(ChatMessage_b64: string) {
            WebviewController.addMessage(WebviewControllerDecoder.decodeChatMessage(ChatMessage_b64));
        },
        setLoaded() {
            DLOG("setLoaded called");
        },
        showLoadingDialog() {
            DLOG("showLoadingDialog called");
        },
        hideLoadingDialog() {
            DLOG("hideLoadingDialog called");
        },
        requestMessageHistory(GetMessagesRequest_b64: string, id: string) {},
        requestUserGroups(id: string) {},
        searchUsers(substring: string, id: string) {}
    } as any;
}
