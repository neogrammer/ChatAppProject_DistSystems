using Microsoft.AspNetCore.SignalR;

namespace _3420_Chat_Service.Hubs
{
    public class CustomUserIdProvider : IUserIdProvider
    {
        public string? GetUserId(HubConnectionContext connection)
        {
            // Extract userId from query string
            return connection.GetHttpContext()?.Request.Query["userId"].ToString();
        }
    }
}
