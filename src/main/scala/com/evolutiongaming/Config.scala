package com.evolutiongaming

object Config {

  private def newAccount(username: String, password: String, role: Models.UserType): (String, Models.Account) =
    username -> Models.Account(username, password, role)

  val accounts = Map(
    newAccount("joe", "secret", Models.User),
    newAccount("jim", "power", Models.Admin),
    newAccount("tom", "heart", Models.User),
    newAccount("bob", "baker", Models.Admin)
  )

}
