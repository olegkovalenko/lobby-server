package com.evolutiongaming

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{FlowShape, OverflowStrategy}

import scala.util.{Failure, Success, Try}
import spray.json._

class Server(lobbyProps: Props)(implicit val system: ActorSystem) extends Directives {
  val lobbyActor = system.actorOf(lobbyProps)

  def flow: Flow[Message, Message, ActorRef] = {
    val clientId = java.util.UUID.randomUUID().toString

    val clientActorSource = Source.actorRef[Protocol.Response](5,OverflowStrategy.fail)
    Flow.fromGraph(GraphDSL.create(clientActorSource) { implicit builder => clientActor =>

      import GraphDSL.Implicits._
      import LobbyJsonProtocol._

      val materialization = builder.materializedValue.map(clientActorRef => (clientId, Protocol.NewClient(clientId, clientActorRef)))
      val merge = builder.add(Merge[(String, Protocol.Request)](2))
      val bcast = builder.add(Broadcast[Try[(String, Protocol.Request)]](2))
      val mergeResponse = builder.add(Merge[Protocol.Response](2))

      val filterInvalid = builder.add(Flow[Try[(String, Protocol.Request)]].collect {
        case Failure(reason) =>
          println("unable to identify request due to a " + reason)
          Protocol.BadRequest
      })
      val filterValid = builder.add(Flow[Try[(String, Protocol.Request)]].collect {
        case Success(request) => request
      })

      val fromWebsocket = builder.add(Flow[Message].collect {
        case TextMessage.Strict(str) =>
          Try((clientId, str.parseJson.convertTo[Protocol.Request]))
      })

      val toWebsocket = builder.add(Flow[Protocol.Response].map {
        case response: Protocol.Response =>
          TextMessage(response.toJson.toString)
      })

      val lobbyActorSink = Sink.actorRef[(String, Protocol.Request)](lobbyActor, (clientId, Protocol.ClientLeft))

                            materialization ~> merge ~> lobbyActorSink
      fromWebsocket ~> bcast ~> filterValid ~> merge
                       bcast ~> filterInvalid ~> mergeResponse ~> toWebsocket
                                  clientActor ~> mergeResponse

      FlowShape(fromWebsocket.in, toWebsocket.out)
    })

  }

  val route: Route =
    path("lobby") {
      get {
        handleWebSocketMessages(flow)
      }
    }

}
