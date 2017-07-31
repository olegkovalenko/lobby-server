package com.evolutiongaming

import akka.actor.{DeadLetter, Props}
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.testkit.TestProbe
import org.scalatest.FunSuite
import spray.json._

import scala.concurrent.duration._

class ServerTest extends FunSuite with ScalatestRouteTest {

  trait ExpectNoDeadLetter { self: TestProbe =>
    def expectNoDeadLetter[T](max: FiniteDuration)(f: => T): Unit = {
      try {
        system.eventStream.subscribe(ref, classOf[DeadLetter])
        f
        expectNoMsg(max)
      } finally system.eventStream.unsubscribe(ref, classOf[DeadLetter])
    }
  }

  def withClient(route: Route)(assertions: WSProbe => Unit): Unit = {
    val client = WSProbe()
    val probe = new TestProbe(system) with ExpectNoDeadLetter

    // to be sure not to leak subscribers
    probe.expectNoDeadLetter(100 milliseconds) {
      WS(s"/lobby", client.flow) ~> route ~> check(assertions(client))
      // don't hang on connection
      client.sendCompletion()
    }
  }

  implicit class ProtocolAwareWSProbe(probe: WSProbe) {
    def send(request: Protocol.Request)(implicit ev: RootJsonFormat[Protocol.Request]): this.type = {
      probe.sendMessage(request.toJson.toString)
      this
    }
    def expect(response: Protocol.Response)(implicit ev: RootJsonFormat[Protocol.Response]): this.type = {
      probe.expectMessage(response.toJson.toString)
      this
    }
    def expect[T <: Protocol.Response: RootJsonFormat]: T = {
      probe.expectMessage() match { case TextMessage.Strict(str) => str.parseJson.convertTo[T] }
    }
  }

  test("usage") {
    import Config.accounts
    import LobbyJsonProtocol._
    import Models._
    import Protocol._

    val server = new Server(Lobby.props)

    withClient(server.route) { joe =>
      // whenever request is meaningless client receives bad request
      Seq("", "{}", "//\\", """{"$type":"unknown"}""").foreach { garbage =>
        joe.sendMessage(garbage)
        joe expect BadRequest
      }
      joe send Login("joe", "forgot") expect LoginFailed
      joe send Login("joe", accounts("joe").password) expect LoginSuccessful()

      joe send Ping(1) expect Pong(1)

      joe send SubscribeTables expect TableList(Vector.empty)

      def notified(notification: Protocol.Response, subscribers: WSProbe*): Unit = { subscribers.foreach(_.expect(notification)) }

      // joe aren't able to add, remove, update table because he is regular user
      joe send AddTable(-1, NewTable("bingo", participants = 5)) expect NotAuthorized
      joe send RemoveTable(1) expect NotAuthorized
      joe send UpdateTable(Table(1, "club", 1)) expect NotAuthorized

      withClient(server.route) { jim =>
        jim send Login("jim", accounts("jim").password) expect LoginSuccessful(Admin)

        val newPokerTable = NewTable("poker", 0)

        val tableAdded = jim.send(AddTable(-1, newPokerTable)).expect[TableAdded]

        notified(tableAdded, joe)

        assert(newPokerTable.name == tableAdded.table.name)
        assert(newPokerTable.participants == tableAdded.table.participants)

        var pokerTable: Table = tableAdded.table

        // addition failure: afterId not found
        jim send AddTable(99, NewTable("whitejack", 3)) expect AdditionFailed

        pokerTable = pokerTable.copy(name = "poker#1", participants = 1)
        jim send UpdateTable(pokerTable) expect TableUpdated(pokerTable)

        notified(TableUpdated(pokerTable), joe)

        // update failure: table id not found
        jim send UpdateTable(pokerTable.copy(id = 100)) expect UpdateFailed(100)

        jim send RemoveTable(pokerTable.id) expect TableRemoved(pokerTable.id)
        notified(TableRemoved(pokerTable.id), joe)

        // remove failure: table with given id not found, it has already been removed
        jim send RemoveTable(pokerTable.id) expect RemovalFailed(pokerTable.id)

        joe send UnsubscribeTables
        joe.expectNoMessage(50 milliseconds)

        withClient(server.route) { tom =>
          tom send Login("tom", accounts("tom").password) expect LoginSuccessful(User)

          joe send SubscribeTables expect TableList(Vector.empty)
          tom send SubscribeTables expect TableList(Vector.empty)

          def listing: Vector[Table] = {
            jim.send(SubscribeTables)
            val tables = jim.expect[TableList].tables
            jim.send(UnsubscribeTables)
            tables
          }

          def addTable(afterId: Int, name: String, participants: Int): Table = {
            jim.send(AddTable(afterId, NewTable(name.toString, participants)))
            val change = jim.expect[TableAdded]
            notified(change, joe, tom)
            change.table
          }

          // check tables prepended correctly
          val ids = (1 to 5).map { i => addTable(-1, i.toString, i).id }
          val tables = listing
          assert(ids.reverse == tables.map(_.id))

          // check tables inserted correctly after given id.
          // each existing table id is followed by id of newly created one
          val seq = tables.flatMap { t => Vector(t.id, addTable(t.id, t.name + "!", t.participants).id) }
          assert(seq == listing.map(_.id))
        }
      }
    }
  }
}
