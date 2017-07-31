# Coding assignment: implement a WebSocket server for a lobby

## Prerequisites

  * java 1.8.0+
  * scala 2.12.1
  * sbt 0.13.0+
  
### Build

    sbt "compile"
    
## Usage

Start server (listens on localhost:9090, to change refer to application.conf keys app.{interface, port})

    sbt "runMain com.evolutiongaming.Main"
    
then connect to "ws://localhost:9090/lobby" using your websocket client of choice 
and make requests according to protocol.

Login using predefined user accounts

|username|password|user type|
|:--------:|:--------:|:---------:|
|joe| secret| user|
|jim| power|  admin|
|tom| heart|  user|
|bob| baker|  admin|

## Documentation

see [assignment.md](assignment.md)

## License

Lobby server is released under the [MIT License](http://www.opensource.org/licenses/MIT).
