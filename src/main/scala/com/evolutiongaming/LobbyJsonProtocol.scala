package com.evolutiongaming

import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

trait LobbyJsonProtocol extends DefaultJsonProtocol {
  import Models._
  import Protocol._
  import java.util.Locale

  // snake case inspired by https://gist.github.com/agemooij/7679130
  import reflect._
  def snakify(name: String) = CamelCased.replaceAllIn(Capitalized.replaceAllIn(name, Underscore), Underscore).toLowerCase(Locale.US)

  override protected def extractFieldNames(classTag: ClassTag[_]) = {
    super.extractFieldNames(classTag).map(snakify(_))
  }

  private val Capitalized = """([A-Z]+)([A-Z][a-z])""".r
  private val CamelCased = """([a-z\d])([A-Z])""".r
  private val Underscore = "$1_$2"

  implicit object UserTypeJsonFormat extends JsonFormat[UserType] {
    def write(obj: UserType) = JsString(obj match {
      case Admin => "admin"
      case User => "user"
    })

    def read(value: JsValue) = value match {
      case JsString(x) =>
        if (x == "admin") Admin
        else if (x == "user") User
        else deserializationError("Expected admin/user as JsString, but got " + x)
      case x => deserializationError("Expected JsString, but got " + x)
    }
  }

  def withHint[T <: Product :ClassTag](rjf: RootJsonFormat[T]): RootJsonFormat[T] = {
    val hint = snakify(classTag[T].runtimeClass.getSimpleName.stripSuffix("$"))

    new RootJsonFormat[T] {
      override def read(json: JsValue): T = rjf.read(json)

      override def write(obj: T): JsValue = {
        JsObject(Map("$type" -> JsString(hint)) ++ rjf.write(obj).asJsObject.fields)
      }
    }
  }

  def constFormat[T](const: T): RootJsonFormat[T] = new RootJsonFormat[T] {
    override def read(json: JsValue): T = const

    override def write(obj: T): JsValue = JsObject()
  }

  implicit val loginFormat = withHint(jsonFormat2(Protocol.Login))
  implicit val loginSuccessfulFormat = withHint(jsonFormat1(Protocol.LoginSuccessful))

  implicit val loginFailedFormat = withHint(constFormat(Protocol.LoginFailed))

  implicit val tableFormat = jsonFormat3(Models.Table)
  implicit val newTableFormat = jsonFormat2(Models.NewTable)
  implicit val tableListFormat = withHint(jsonFormat1(Protocol.TableList))

  implicit val pingFormat = withHint(jsonFormat1(Protocol.Ping))
  implicit val pongFormat = withHint(jsonFormat1(Protocol.Pong))

  implicit val subscribeTablesFormat = withHint(constFormat(Protocol.SubscribeTables))

  implicit val tableAddedFormat = withHint(jsonFormat2(Protocol.TableAdded))
  implicit val tableRemovedFormat = withHint(jsonFormat1(Protocol.TableRemoved))
  implicit val tableUpdatedFormat = withHint(jsonFormat1(Protocol.TableUpdated))

  implicit val unsubscribeTablesFormat = withHint(constFormat(Protocol.UnsubscribeTables))

  implicit val notAuthorizedFormat = withHint(constFormat(Protocol.NotAuthorized))

  implicit val addTableFormat = withHint(jsonFormat2(Protocol.AddTable))

  implicit val updateTableFormat = withHint(jsonFormat1(Protocol.UpdateTable))
  implicit val remoteTableFormat = withHint(jsonFormat1(Protocol.RemoveTable))
  implicit val removalFailedFormat = withHint(jsonFormat1(Protocol.RemovalFailed))
  implicit val updateFailedFormat = withHint(jsonFormat1(Protocol.UpdateFailed))
  implicit val additionFailedFormat = withHint(constFormat(Protocol.AdditionFailed))
  implicit val badRequestFailedFormat = withHint(constFormat(Protocol.BadRequest))

  implicit object ResponseFormat extends RootJsonFormat[Protocol.Response] {

    override def read(json: JsValue): Protocol.Response = json match {
      case JsObject(fields) if fields.contains("$type") =>
        val JsString(hint) = fields("$type")

        hint match {
          case "login_successful" => loginSuccessfulFormat.read(json)
          case "login_failed" => loginFailedFormat.read(json)
          case "pong" => pongFormat.read(json)
          case "table_list" => tableListFormat.read(json)
          case "table_added" => tableAddedFormat.read(json)
          case "table_removed" => tableRemovedFormat.read(json)
          case "table_updated" => tableUpdatedFormat.read(json)
          case "not_authorized" => notAuthorizedFormat.read(json)
          case "removal_failed" => removalFailedFormat.read(json)
          case "update_failed" => updateFailedFormat.read(json)
          case "addition_failed" => additionFailedFormat.read(json)
          case "bad_request" => badRequestFailedFormat.read(json)
          case x => deserializationError("Unknown $type " + x)
        }
      case x => deserializationError("Expected JsObject, but got " + x)
    }

    override def write(obj: Protocol.Response): JsValue = {
      obj match {
        case x: LoginSuccessful => loginSuccessfulFormat.write(x)
        case x: LoginFailed.type => loginFailedFormat.write(x)
        case x: Pong => pongFormat.write(x)
        case x: TableList => tableListFormat.write(x)
        case x: TableAdded => tableAddedFormat.write(x)
        case x: TableRemoved => tableRemovedFormat.write(x)
        case x: TableUpdated => tableUpdatedFormat.write(x)
        case x: NotAuthorized.type => notAuthorizedFormat.write(x)
        case x: RemovalFailed => removalFailedFormat.write(x)
        case x: UpdateFailed => updateFailedFormat.write(x)
        case x: AdditionFailed.type => additionFailedFormat.write(x)
        case x: BadRequest.type => badRequestFailedFormat.write(x)
      }
    }
  }

  implicit object RequestFormat extends RootJsonFormat[Protocol.Request] {
    override def read(json: JsValue): Request = json match {
      case JsObject(fields) if fields.contains("$type") =>
        val JsString(hint) = fields("$type")

        hint match {
          case "login" => loginFormat.read(json)
          case "ping" => pingFormat.read(json)
          case "subscribe_tables" => subscribeTablesFormat.read(json)
          case "unsubscribe_tables" => unsubscribeTablesFormat.read(json)
          case "add_table" => addTableFormat.read(json)
          case "update_table" => updateTableFormat.read(json)
          case "remove_table" => remoteTableFormat.read(json)
          case x => deserializationError("Unknown $type " + x)
        }
      case x => deserializationError("Expected JsObject, but got " + x)
    }

    override def write(obj: Request): JsValue = {
      obj match {
        case x: Login => loginFormat.write(x)
        case x: Ping => pingFormat.write(x)
        case x: SubscribeTables.type => subscribeTablesFormat.write(x)
        case x: UnsubscribeTables.type => unsubscribeTablesFormat.write(x)
        case x: AddTable => addTableFormat.write(x)
        case x: UpdateTable => updateTableFormat.write(x)
        case x: RemoveTable => remoteTableFormat.write(x)
      }
    }
  }
}

object LobbyJsonProtocol extends LobbyJsonProtocol