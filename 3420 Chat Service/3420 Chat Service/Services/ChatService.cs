using Grpc.Core;
using Grpc.Net.Client;
using Microsoft.AspNetCore.SignalR;
using _3420_Chat_Service.Hubs;
using _3420_Chat_Service.Auth;

namespace _3420_Chat_Service.Services
{
    public class ChatGrpcService : _3420_Chat_Service.ChatService.ChatServiceBase
    {
        private readonly IHubContext<ChatHub> _hubContext;
        private readonly ILogger<ChatGrpcService> _logger;
        private readonly AuthService.AuthServiceClient _authClient;

        public ChatGrpcService(IHubContext<ChatHub> hubContext, ILogger<ChatGrpcService> logger, IConfiguration configuration)
        {
            _hubContext = hubContext;
            _logger = logger;
            
            var authServerUrl = configuration["AuthServer:Url"] ?? "http://24.236.104.52r:55101";
            var channel = GrpcChannel.ForAddress(authServerUrl);
            _authClient = new AuthService.AuthServiceClient(channel);
        }

        public override async Task<SendMessageResponse> SendMessage(ChatMessage request, ServerCallContext context)
        {
            try
            {
                var authHeader = context.RequestHeaders.GetValue("authorization");
                if (string.IsNullOrEmpty(authHeader) || !authHeader.StartsWith("Bearer "))
                {
                    _logger.LogWarning("Missing or invalid authorization header");
                    return new SendMessageResponse { Success = false };
                }

                var token = authHeader.Substring(7);
                var authRequest = new ValidateTokenRequest { Token = token };
                var authResponse = await _authClient.ValidateTokenAsync(authRequest);

                if (!authResponse.Valid)
                {
                     _logger.LogWarning("Invalid token provided");
                    return new SendMessageResponse { Success = false };
                }

                _logger.LogInformation("Received message from {UserName} in room {RoomId}: {Content}", 
                    request.UserName, request.RoomId, request.Content);

                await _hubContext.Clients.Group(request.RoomId).SendAsync("ReceiveMessage", request);

                _logger.LogInformation("Message broadcast successfully to room {RoomId} via SignalR", request.RoomId);

                return new SendMessageResponse { Success = true };
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error processing message: {Error}", ex.Message);
                return new SendMessageResponse { Success = false };
            }
        }
    }
}