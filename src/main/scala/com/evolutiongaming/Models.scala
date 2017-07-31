package com.evolutiongaming

object Models {
  sealed trait UserType
  case object Admin extends UserType
  case object User extends UserType

  case class NewTable(name: String, participants: Int)
  case class Table(id: Int, name: String, participants: Int)

  case class Account(username: String, password: String, role: UserType)
}
