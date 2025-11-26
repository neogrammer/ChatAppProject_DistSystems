# gRPC Auth Service (Java + MySQL)

## Build & Run with Docker (no Gradle installed required)
```bash
cd grpc-auth-mysql-server
docker compose up --build
```
Service on tcp 50051, MySQL on 3306.

## Smoke test with grpcurl
```bash
grpcurl -plaintext -d '{"email":"justin@example.com","password":"Secret123!","displayName":"Justin"}'   localhost:50051 chat.auth.v1.AuthService/Register

grpcurl -plaintext -d '{"email":"justin@example.com","password":"Secret123!"}'   localhost:50051 chat.auth.v1.AuthService/Login
```