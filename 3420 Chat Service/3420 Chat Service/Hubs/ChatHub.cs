using Microsoft.AspNetCore.SignalR;

namespace _3420_Chat_Service.Hubs
{
    public class ChatHub : Hub
    {
        public async Task SendMessage(ChatMessage message)
        {
            // Broadcast to specific group/room
            await Clients.Group(message.RoomId).SendAsync("ReceiveMessage", message);
        }

        public async Task JoinGroup(string groupId)
        {
            await Groups.AddToGroupAsync(Context.ConnectionId, groupId);
            Console.WriteLine($"Client {Context.ConnectionId} joined group {groupId}");
        }

        public async Task LeaveGroup(string groupId)
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, groupId);
            Console.WriteLine($"Client {Context.ConnectionId} left group {groupId}");
        }

        public override Task OnConnectedAsync()
        {
            var connectionId = Context.ConnectionId;
            Console.WriteLine($"SignalR client connected: {connectionId}");
            return base.OnConnectedAsync();
        }

        public override Task OnDisconnectedAsync(Exception? exception)
        {
            var connectionId = Context.ConnectionId;
            Console.WriteLine($"SignalR client disconnected: {connectionId}");
            return base.OnDisconnectedAsync(exception);
        }
    }
}