# Project Context

## Directory Structure
- `/mnt/c/Users/jhela/repos/3420_group/ChatAppProject_DistSystems` = Group's current progress on assignment
- `/mnt/c/Users/jhela/repos/3420_group/RealTimeChat` = Charles's personal reference project to rip code from
- `/mnt/c/Users/jhela/RiderProjects/3420 Chat Service` = Charles's local copy that needs to be moved to group repo

## Charles's Role
- Responsible for real-time messaging using SignalR
- Architecture: Client → gRPC → SignalR broadcast + DB save
- Needs Redis backplane for horizontal scaling
- Has not started yet (self-described as "lazy as shit lol")

## Team Responsibilities
- Justin Hardee: Auth system, database setup, backend endpoints
- Charles Stokes: Real-time gRPC messaging system, pub/sub integration, message persistence
- Tyler Wray: Android UI, frontend integration, offline caching