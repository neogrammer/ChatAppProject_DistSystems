// [PROTOBUF-MOCK-TYPE] Encapsulates a chat message.
export interface IProtobufChatMessage {
    getId(): string;
    getRoomId(): string;
    getUserId(): string;
    getContent(): string;
    getCreatedAt(): number;

    // needed extra fields in proto:
    getUserName(): string;
}