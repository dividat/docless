package swala

import akka.http.scaladsl.model.StatusCode

object Responses {
  type HeaderName = String
  case class Header(`type`: Type,
                    format: Option[Format] = None,
                    description: Option[String] = None)

  case class Response(description: String,
                      headers: Map[HeaderName, Header] = Map.empty,
                      schema: Option[SchemaDefinition] = None,
                      example: Option[String] = None)
}

case class Responses(default: Responses.Response,
                     byStatusCode: Map[Int, Responses.Response] = Map.empty)