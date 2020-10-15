package de.bwhc.mtb.api


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
  Result
}
import play.api.libs.json.Json.toJson

import de.ekut.tbi.generators.Gen

import de.bwhc.mtb.data.entry.dtos.MTBFile
import de.bwhc.mtb.data.gens._



class ExampleProvider @Inject()(
  val controllerComponents: ControllerComponents
)(
  implicit ec: ExecutionContext
)
extends BaseController
{
  
  implicit val rnd = new scala.util.Random(42)

  def mtbfile: Action[AnyContent] =
    Action.async {
      Future.successful(
        Gen.of[MTBFile].next
      )
      .map(toJson(_))
      .map(Ok(_))
    }

}
