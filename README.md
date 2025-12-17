# Distributed Chat App
Contributers: Justin Hardee, Tyler Wray, Charles Stokes
## Components
### Android Client
The android client lives in the root project directory. Make sure the Chat Service is running (see below), then, to build, load the project into Android Studio and build the app (note that it may take a few minutes and may appear to hang while running the npm scripts). Initially, the user has no chat rooms and is just shown their empty room list. To create a new chat room, tap the plus sign at the top right and go from there. Creating a room requires multiple users, so make sure to have launched the app and registered a few accounts, then search them by display name. Added users are forcefully added to created chat rooms.
### Chat Service
The Chat Service can be found in the "3420 Chat Service" directory. To build, use "docker compose up --build" in the project root directory (Docker Desktop must be running). This will also start the Auth Service, so there's no need to start it individually. The client connects to the Chat Service through the android emulator local host IP so the device must be running on your local machine to function. In a real deployment, the IP address would simply be changed to the endpoint where the services are hosted at. It supports horizontal scaling through use of a redis backplane.
### Auth Service
The Auth Service lives in the "grpc-auth-mysql-server" directory. To build it individually, use the "docker compose up --build" in its directory, though it'll be launched as part of the Chat Service when you run "docker compose up --build" in the project root directory. 
## Notes
- We moved message persistence from Milestone 2 to Milestone 3
- Retries and disconnect handling has also been moved to Milestone 3, so ensure that Chat Service is running before attempting to log in
