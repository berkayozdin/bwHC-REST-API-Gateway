package de.bwhc.rest.auth



import javax.inject.Inject


import scala.concurrent.{ExecutionContext,Future}

import play.api.mvc.{
  Action,
  ActionFilter,
  AnyContent,
  BaseController,
  Request
}


trait AuthorizationActions
{

  this: BaseController =>


  def PermissionCheck[R[_]](
    check: R[_] => Boolean
  )(
    implicit ec: ExecutionContext
  ) = new ActionFilter[R]{

    override def executionContext = ec

    override def filter[T](req: R[T]) =
      Future.successful {
        if (check(req)) None // OK, no objections... 
        else Some(Forbidden)
      }

  }

  def AsyncPermissionCheck[R[_]](
    check: R[_] => Future[Boolean]
  )(
    implicit ec: ExecutionContext
  ) = new ActionFilter[R]{

    override def executionContext = ec

    override def filter[T](req: R[T]) =
      for {
        ok <- check(req)
        result = if (ok) None else Some(Forbidden)
      } yield result

  }


}
