using Grpc.Core;
using Grpc.Net.Client;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using _3420_Chat_Service.Hubs;
using _3420_Chat_Service.Auth;
using _3420_Chat_Service.Data;
using _3420_Chat_Service.Models;

namespace _3420_Chat_Service.Services
{
    public class ChatGrpcService : _3420_Chat_Service.ChatService.ChatServiceBase
    {
        private readonly IHubContext<ChatHub> _hubContext;
        private readonly ILogger<ChatGrpcService> _logger;
        private readonly AuthService.AuthServiceClient _authClient;
        private readonly AppDbContext _dbContext;

        public ChatGrpcService(IHubContext<ChatHub> hubContext, ILogger<ChatGrpcService> logger, IConfiguration configuration, AppDbContext dbContext)
        {
            _hubContext = hubContext;
            _logger = logger;
            _dbContext = dbContext;
            
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

                // Save message to database
                var message = new Message
                {
                    UserId = request.UserId,
                    UserName = request.UserName,
                    GroupId = int.Parse(request.RoomId),
                    Content = request.Content,
                    SentAt = DateTime.UtcNow
                };

                _dbContext.Messages.Add(message);
                await _dbContext.SaveChangesAsync();

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

        public override async Task<GetMessagesResponse> GetMessages(GetMessagesRequest request, ServerCallContext context)
        {
            try
            {
                var messages = await _dbContext.Messages
                    .Where(m => m.GroupId == request.GroupId)
                    .OrderByDescending(m => m.SentAt)
                    .Select(m => new ChatMessage
                    {
                        Id = m.Id.ToString(),
                        RoomId = m.GroupId.ToString(),
                        UserId = m.UserId,
                        UserName = m.UserName,
                        Content = m.Content,
                        CreatedAt = ((DateTimeOffset)m.SentAt).ToUnixTimeMilliseconds()
                    })
                    .ToListAsync();

                var response = new GetMessagesResponse();
                response.Messages.AddRange(messages);
                return response;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error getting messages: {Error}", ex.Message);
                return new GetMessagesResponse();
            }
        }

        public override async Task<CreateGroupResponse> CreateGroup(CreateGroupRequest request, ServerCallContext context)
        {
            try
            {
                var group = new Group
                {
                    GroupName = request.GroupName
                };

                _dbContext.Groups.Add(group);
                await _dbContext.SaveChangesAsync();

                // Add creator as first group member
                var groupMember = new GroupMember
                {
                    GroupId = group.Id,
                    UserId = request.UserId
                };

                _dbContext.GroupMembers.Add(groupMember);
                await _dbContext.SaveChangesAsync();

                return new CreateGroupResponse 
                { 
                    Success = true, 
                    GroupId = group.Id 
                };
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating group: {Error}", ex.Message);
                return new CreateGroupResponse { Success = false };
            }
        }

        public override async Task<AddUserToGroupResponse> AddUserToGroup(AddUserToGroupRequest request, ServerCallContext context)
        {
            try
            {
                // TODO: Add user validation - check if userId exists in auth service
                
                // Check if user is already in group
                var existingMember = await _dbContext.GroupMembers
                    .FirstOrDefaultAsync(gm => gm.GroupId == request.GroupId && gm.UserId == request.UserId);

                if (existingMember != null)
                {
                    return new AddUserToGroupResponse { Success = false };
                }

                var groupMember = new GroupMember
                {
                    GroupId = request.GroupId,
                    UserId = request.UserId
                };

                _dbContext.GroupMembers.Add(groupMember);
                await _dbContext.SaveChangesAsync();

                return new AddUserToGroupResponse { Success = true };
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error adding user to group: {Error}", ex.Message);
                return new AddUserToGroupResponse { Success = false };
            }
        }

        public override async Task<GetUserGroupsResponse> GetUserGroups(GetUserGroupsRequest request, ServerCallContext context)
        {
            try
            {
                var groups = await _dbContext.GroupMembers
                    .Where(gm => gm.UserId == request.UserId)
                    .Include(gm => gm.Group)
                    .Select(gm => new GroupInfo
                    {
                        Id = gm.Group.Id,
                        GroupName = gm.Group.GroupName
                    })
                    .ToListAsync();

                var response = new GetUserGroupsResponse();
                response.Groups.AddRange(groups);
                return response;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error getting user groups: {Error}", ex.Message);
                return new GetUserGroupsResponse();
            }
        }
    }
}
