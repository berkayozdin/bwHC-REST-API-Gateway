package de.bwhc.session




import scala.concurrent.{
  Future,
  ExecutionContext
}

import javax.inject.Inject

import play.api.mvc.{
  Action,
  AnyContent,
  BaseController,
  ControllerComponents,
  Request,
}
import play.api.libs.json.{
  Json, JsValue, Format
}



class SessionController @Inject()(
  val controllerComponents: ControllerComponents
)(
  implicit ec: ExecutionContext
)
extends BaseController
{


  def login(
    username: String,
    password: String
  ): Action[AnyContent] = Action.async {

    Future.successful {

      //TODO


      Ok
    }

  }


}
