using _3420_Chat_Service.Services;
using _3420_Chat_Service.Hubs;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddGrpc();
builder.Services.AddSignalR();

// Add CORS for development (WebView connections)
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

var app = builder.Build();

// Configure the HTTP request pipeline.
app.UseCors();

app.MapGrpcService<GreeterService>();
app.MapGrpcService<ChatGrpcService>();
app.MapHub<ChatHub>("/chathub");

app.MapGet("/",
    () =>
        "Communication with gRPC endpoints must be made through a gRPC client. To learn how to create a client, visit: https://go.microsoft.com/fwlink/?linkid=2086909");

app.Run();