package com.evolutiongaming

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import Protocol._

class Lobby(accounts: Map[String, Models.Account]) extends Actor with ActorLogging {

  private val db = new Database
  private var subscribers: Vector[ActorRef] = Vector.empty
  private var authenticated: Map[ActorRef, Models.Account] = Map.empty

  // client id to it's actor reference, since sender refers to some stage of flow
  private var targets: Map[String, ActorRef] = Map.empty

  override def receive: Receive = LoggingReceive {
    case (_, NewClient(id, target)) =>
      targets += id -> target

    case (clientId: String, Login(username, password)) =>
      val target = targets(clientId)

      val optAccount: Option[Models.Account] = accounts.get(username).filter(_.password == password)
      optAccount.foreach { account => authenticated += target -> account }
      val optResponse = optAccount.map(account => LoginSuccessful(account.role))
      target ! optResponse.getOrElse(LoginFailed)

    case (clientId: String, Ping(seq)) =>
      val target = targets(clientId)
      withAuthorization(target, Models.User, Models.Admin) {
        target ! Pong(seq)
      }

    case (clientId: String, SubscribeTables) =>
      val target = targets(clientId)

      withAuthorization(target, Models.User, Models.Admin) {
        if (!subscribers.contains(target)) {
          subscribers = subscribers :+ target
        }
        target ! TableList(db.tables)
      }

    case (clientId: String, UnsubscribeTables) =>
      val target = targets(clientId)

      withAuthorization(target, Models.User, Models.Admin) {
        unsubscribe(target)
      }

    // privileged commands

    case (clientId: String, AddTable(afterId, table)) =>
      val target = targets(clientId)

      withAuthorization(target, Models.Admin) {
        val inserted = db.insert(table, afterId)
        foldDbResult(inserted)(AdditionFailed)(t => TableAdded(afterId, t))(target)
      }


    case (clientId: String, UpdateTable(table)) =>
      val target = targets(clientId)

      withAuthorization(target, Models.Admin) {
        val updated = db.update(table)
        foldDbResult(updated)(UpdateFailed(table.id))(TableUpdated)(target)
      }


    case (clientId: String, RemoveTable(id)) =>
      val target = targets(clientId)

      withAuthorization(target, Models.Admin) {
        val removed = db.remove(id)
        foldDbResult(removed)(RemovalFailed(id))(_ => TableRemoved(id))(target)
      }

    case (clientId: String, ClientLeft) =>
      val target = targets(clientId)
      authenticated -= target
      unsubscribe(target)
      targets -= clientId

  }

  private def unsubscribe(target: ActorRef) = {
    subscribers = subscribers.filter(_ != target)
  }

  private def withAuthorization(target: ActorRef, roles: Models.UserType*)(action: => Unit): Unit = {
    authenticated.get(target)
      .filter(account => roles.contains(account.role))
      .map(_ => action)
      .getOrElse(target ! NotAuthorized)
  }

  private def notifySubscribers(response: Response): Unit = subscribers.foreach(_ ! response)

  private def foldDbResult[T](result: Option[T])(ifEmpty: => Protocol.Response)(f: T => Protocol.Response)(target: ActorRef): Unit = {
    result match {
      case None =>
        target ! ifEmpty
      case Some(value) =>
        val response = f(value)
        target ! response
        notifySubscribers(response)
    }
  }
}

object Lobby {
  def props: Props = Props(classOf[Lobby], Config.accounts)
}
