package de.bwhc.rest.util



import scala.util.Either

import scala.concurrent.{
  ExecutionContext,
  Future
}

import play.api.mvc.{
  Action,
  ActionRefiner,
  AnyContent,
  BaseController,
  Request,
  Result
}

import play.api.libs.json._
import Json.toJson

import cats.Id
import cats.data.Ior
import cats.syntax.either._


trait RequestOps
{

  this: BaseController =>


  def errorsOrJson[T: Reads]: Request[AnyContent] => Either[Result,T] = {
    _.body
      .asJson
      .fold(
        BadRequest("Missing or Invalid JSON body").asLeft[T]
      )(
        _.validate[T]
         .asEither
         .leftMap(Outcome.fromJsErrors(_))
         .leftMap(toJson(_))
         .leftMap(BadRequest(_))
      )

    }


  implicit class EitherResultHelpers[T](val f: Request[AnyContent] => Either[Result,T]){

    def thenApply(
      g: T => Future[Result]
    )(
      implicit ec: ExecutionContext
    ): Request[AnyContent] => Future[Result] = {
      req =>
        f(req).fold(
          Future.successful(_),
          g(_)
        )
    }

  }



  def JsonAction[T: Reads](
    block: T => Future[Result]
  )(
    implicit
    ec: ExecutionContext
  ): Action[AnyContent] =
    Action.async { req =>

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
             block(_)
           )
        )

    }



  implicit class OptionOps[T: Writes](opt: Option[T])
  {

    def toJsonOrElse(err: => String) = 
      opt.map(toJson(_))
        .map(Ok(_))
        .getOrElse(NotFound(err)) 

  }



  implicit class EitherOutcomeOps[T](xor: Either[Outcome,T])(
    implicit
    we: Writes[Outcome],
    wt: Writes[T]
  ){

    def toJsonResult: Result = {
       xor.bimap(
        toJson(_),
        toJson(_)
      )
      .fold(
        out => InternalServerError(out),
        Ok(_)
      )
     }

  }


  implicit class IorOutcomeOps[T](ior: Ior[Outcome,T])(
    implicit
    we: Writes[Outcome],
    wt: Writes[T]
  ){

    def toJsonResult: Result = {
       ior.bimap(
        toJson(_),
        toJson(_)
      )
      .fold(
        out => InternalServerError(out),
        Ok(_),
        (out,_) => InternalServerError(out),
      )

     }

  }


}

