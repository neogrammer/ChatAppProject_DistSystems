# Distributed Chat App
Contributers: Justin Hardee, Tyler Wray, Charles Stokes
## Components
### Android Client
The android client lives in the root project directory. To build, load the project into Android Studio and build that way
### Chat Service
The Chat Service can be found in the "3420 Chat Service" directory. To build, use the docker-compose in the project root directory. The client connects the the Chat Service through the android local host IP so it must be running on your local machine to function. It supports horizontal scaling through use of a redis backplane
### Auth Service
The Auth Service lives in the "grpc-auth-mysql-server" directory. To build, use the docker-compose in its directory. The client connect to this using a public server at "24.236.104.52" so no need to run and build on your machine.
## Notes
- We moved message persistence from Milestone 2 to Milestone 3
- Retries and disconnect handling has also been moved to Milestone 3, so ensure that Chat Service is running before attempting to log in
