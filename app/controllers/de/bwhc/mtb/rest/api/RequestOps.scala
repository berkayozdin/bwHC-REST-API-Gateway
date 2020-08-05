package de.bwhc.mtb.rest.api



import scala.concurrent.{
  ExecutionContext,
  Future
}

import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  Request,
  Result
}

import play.api.libs.json._
import Json.toJson


import cats.syntax.either._


abstract class RequestOps extends BaseController
{

  def processJson[T: Reads](
    f: T => Future[Result]
  )(
    implicit
    req: Request[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {

    req.body
      .asJson
      .fold(
        Future.successful(BadRequest("Missing or Invalid JSON body"))
      )(
        _.validate[T]
         .asEither
         .leftMap(Outcome.fromJsErrors(_))
         .leftMap(toJson(_))
         .fold(
           out => Future.successful(BadRequest(out)),
           f(_)
         )
      )

  }


  def toJsonOrElse[T: Writes](
    th: => Future[Option[T]]
  )(
    err: String
  )(
    implicit
    ec: ExecutionContext
  ): Future[Result] = {

    for {
      opt    <- th
      json   =  opt.map(toJson(_))
      result =  json.fold(NotFound(err))(Ok(_))
    } yield result

  }

}

