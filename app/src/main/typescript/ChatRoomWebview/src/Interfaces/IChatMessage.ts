export interface IChatMessageRoomUnaware {
    readonly modifiedAt?: number;
    readonly id: string;
    readonly userId: string;
    readonly content: string;
    readonly createdAt: number;
    readonly userName: string;
    roomId?: string;
    roomName?: string;
}

export interface IChatMessage extends IChatMessageRoomUnaware {
    readonly roomId: string;
    readonly roomName: string;
}