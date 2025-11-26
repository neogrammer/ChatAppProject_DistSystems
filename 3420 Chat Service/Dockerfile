FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY ["3420 Chat Service/3420 Chat Service.csproj", "3420 Chat Service/"]
RUN dotnet restore "3420 Chat Service/3420 Chat Service.csproj"

COPY . .
WORKDIR "/src/3420 Chat Service"
RUN dotnet build "3420 Chat Service.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "3420 Chat Service.csproj" -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS final
WORKDIR /app
COPY --from=publish /app/publish .

EXPOSE 5065
EXPOSE 5066

ENTRYPOINT ["dotnet", "3420 Chat Service.dll"]
