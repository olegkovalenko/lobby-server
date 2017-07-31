package com.evolutiongaming

import akka.actor.ActorRef

object Protocol {
  import Models._

  sealed trait Request
  sealed trait Response

  case class Login(username: String, password: String) extends Request

  case class LoginSuccessful(userType: UserType = User) extends Response
  case object LoginFailed extends Response

  case class Ping(seq: Long) extends Request
  case class Pong(seq: Long) extends Response


  case object SubscribeTables extends Request

  case class TableList(tables: Vector[Table]) extends Response
  case class TableAdded(afterId: Int, table: Table) extends Response
  case class TableRemoved(id: Int) extends Response
  case class TableUpdated(table: Table) extends Response

  case object UnsubscribeTables extends Request

  case object NotAuthorized extends Response

  case class AddTable(afterId: Int, table: NewTable) extends Request
  case class UpdateTable(table: Table) extends Request
  case class RemoveTable(id: Int) extends Request

  case class RemovalFailed(id: Int) extends Response
  case class UpdateFailed(id: Int) extends Response
  case object AdditionFailed extends Response

  // whenever json is malformed or doesn't correspond to protocol spec
  case object BadRequest extends Response

  // in order to signal new connection and client leaving
  case class NewClient(id: String, ref: ActorRef) extends Request
  case object ClientLeft extends Request

}
