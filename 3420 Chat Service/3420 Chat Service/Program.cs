using _3420_Chat_Service.Services;
using _3420_Chat_Service.Hubs;
using _3420_Chat_Service.Data;
using Microsoft.AspNetCore.Server.Kestrel.Core;
using Microsoft.EntityFrameworkCore;

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

// Add Entity Framework
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));

builder.Services.AddGrpc();
builder.Services.AddSignalR()
    .AddStackExchangeRedis(builder.Configuration.GetConnectionString("Redis") ?? "localhost:6379");

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

// Auto-apply migrations
using (var scope = app.Services.CreateScope())
{
    var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    dbContext.Database.Migrate();
}

// Configure the HTTP request pipeline.
app.UseCors();

app.MapGrpcService<GreeterService>();
app.MapGrpcService<ChatGrpcService>();
app.MapHub<ChatHub>("/chathub");

app.MapGet("/",
    () =>
        "Communication with gRPC endpoints must be made through a gRPC client. To learn how to create a client, visit: https://go.microsoft.com/fwlink/?linkid=2086909");

app.Run();