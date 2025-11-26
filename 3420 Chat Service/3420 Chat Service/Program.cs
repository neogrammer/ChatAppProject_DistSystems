using _3420_Chat_Service.Services;
using _3420_Chat_Service.Hubs;
using Microsoft.AspNetCore.Server.Kestrel.Core;

var builder = WebApplication.CreateBuilder(args);

// configure kestrel to support both http/1.1 (signalr) and http/2 (grpc)
builder.WebHost.ConfigureKestrel(options =>
{
    // grpc endpoint - http/2 only (plaintext for dev)
    options.ListenAnyIP(5065, listenOptions =>
    {
        listenOptions.Protocols = HttpProtocols.Http2;
    });

    // signalr/http endpoint - http/1.1
    options.ListenAnyIP(5066, listenOptions =>
    {
        listenOptions.Protocols = HttpProtocols.Http1;
    });
});

builder.Services.AddGrpc();
builder.Services.AddSignalR();

// Add CORS for development (WebView connections)
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.WithOrigins("https://appassets.androidplatform.net")
              .AllowAnyHeader()
              .AllowAnyMethod()
              .AllowCredentials();
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