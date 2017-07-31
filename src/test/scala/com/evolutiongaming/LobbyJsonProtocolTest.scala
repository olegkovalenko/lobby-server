package com.evolutiongaming

import org.scalatest.FunSuite
import spray.json._
import LobbyJsonProtocol._

class LobbyJsonProtocolTest extends FunSuite {
  test("roundtrip") {

    def roundtrip[T](json: String, obj: T)(implicit ev: RootJsonFormat[T]): Unit = {
      val serialized = obj.toJson.toString
      assert(json == serialized)
      val obj1: T = ev.read(serialized.parseJson)
      assert(obj1 == obj)
    }

    roundtrip("""{"$type":"login","username":"user1234","password":"password1234"}""", Protocol.Login("user1234", "password1234") : Protocol.Request)
    roundtrip("""{"$type":"login_successful","user_type":"admin"}""", Protocol.LoginSuccessful(Models.Admin): Protocol.Response)
    roundtrip("""{"$type":"login_successful","user_type":"user"}""", Protocol.LoginSuccessful(Models.User))

    roundtrip("""{"$type":"login_failed"}""", Protocol.LoginFailed)
    roundtrip("""{"$type":"login_failed"}""", Protocol.LoginFailed: Protocol.Response)

    roundtrip("""{"$type":"ping","seq":1}""", Protocol.Ping(1))
    roundtrip("""{"$type":"ping","seq":2}""", Protocol.Ping(2): Protocol.Request)

    roundtrip("""{"$type":"pong","seq":1}""", Protocol.Pong(1))
    roundtrip("""{"$type":"pong","seq":2}""", Protocol.Pong(2): Protocol.Response)

    roundtrip("""{"$type":"subscribe_tables"}""", Protocol.SubscribeTables)
    roundtrip("""{"$type":"subscribe_tables"}""", Protocol.SubscribeTables: Protocol.Request)

    val tableList0 = Protocol.TableList(Vector(Models.Table(1, "blackjack", 4)))
    roundtrip("""{"$type":"table_list","tables":[{"id":1,"name":"blackjack","participants":4}]}""", tableList0)
    roundtrip("""{"$type":"table_list","tables":[{"id":1,"name":"blackjack","participants":4}]}""", tableList0: Protocol.Response)

    val tableAdded = Protocol.TableAdded(2, Models.Table(2, "poker", 3))
    roundtrip("""{"$type":"table_added","after_id":2,"table":{"id":2,"name":"poker","participants":3}}""", tableAdded)
    roundtrip("""{"$type":"table_added","after_id":2,"table":{"id":2,"name":"poker","participants":3}}""", tableAdded: Protocol.Response)

    val tableRemoved = Protocol.TableRemoved(3)
    roundtrip("""{"$type":"table_removed","id":3}""", tableRemoved)
    roundtrip("""{"$type":"table_removed","id":3}""", tableRemoved: Protocol.Response)

    val tableUpdated = Protocol.TableUpdated(Models.Table(5, "poker", 7))
    roundtrip("""{"$type":"table_updated","table":{"id":5,"name":"poker","participants":7}}""", tableUpdated)
    roundtrip("""{"$type":"table_updated","table":{"id":5,"name":"poker","participants":7}}""", tableUpdated: Protocol.Response)

    roundtrip("""{"$type":"unsubscribe_tables"}""", Protocol.UnsubscribeTables)
    roundtrip("""{"$type":"unsubscribe_tables"}""", Protocol.UnsubscribeTables: Protocol.Request)

    roundtrip("""{"$type":"not_authorized"}""", Protocol.NotAuthorized)
    roundtrip("""{"$type":"not_authorized"}""", Protocol.NotAuthorized: Protocol.Response)

    val addTable = Protocol.AddTable(3, Models.NewTable("baccarat", 0))
    roundtrip("""{"$type":"add_table","after_id":3,"table":{"name":"baccarat","participants":0}}""", addTable)
    roundtrip("""{"$type":"add_table","after_id":3,"table":{"name":"baccarat","participants":0}}""", addTable: Protocol.Request)

    val updateTable = Protocol.UpdateTable(Models.Table(5, "faro", 1))
    roundtrip("""{"$type":"update_table","table":{"id":5,"name":"faro","participants":1}}""", updateTable)
    roundtrip("""{"$type":"update_table","table":{"id":5,"name":"faro","participants":1}}""", updateTable: Protocol.Request)

    roundtrip("""{"$type":"remove_table","id":2}""", Protocol.RemoveTable(2))
    roundtrip("""{"$type":"remove_table","id":3}""", Protocol.RemoveTable(3): Protocol.Request)

    roundtrip("""{"$type":"removal_failed","id":4}""", Protocol.RemovalFailed(4))
    roundtrip("""{"$type":"removal_failed","id":5}""", Protocol.RemovalFailed(5): Protocol.Response)

    roundtrip("""{"$type":"update_failed","id":8}""", Protocol.UpdateFailed(8))
    roundtrip("""{"$type":"update_failed","id":9}""", Protocol.UpdateFailed(9): Protocol.Response)

    roundtrip("""{"$type":"addition_failed"}""", Protocol.AdditionFailed)
    roundtrip("""{"$type":"addition_failed"}""", Protocol.AdditionFailed: Protocol.Response)

    roundtrip("""{"$type":"bad_request"}""", Protocol.BadRequest)
    roundtrip("""{"$type":"bad_request"}""", Protocol.BadRequest: Protocol.Response)
  }
}
