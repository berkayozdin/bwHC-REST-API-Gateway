package de.bwhc.fake.data


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
  Result,
  Accepting
}
import play.api.libs.json.Json.toJson
import de.ekut.tbi.generators.Gen
import de.bwhc.mtb.dtos.MTBFile
import de.bwhc.mtb.dto.gens._
import org.hl7.fhir.r4.FHIRJson._
import de.bwhc.fhir.MTBFileBundle
import de.bwhc.fhir.Mappings._



class ExampleProvider @Inject()(
  val controllerComponents: ControllerComponents
)(
  implicit ec: ExecutionContext
)
extends BaseController
{
  
  private val FHIR_JSON = "application/fhir+json"

  private val AcceptsFHIR = Accepting(FHIR_JSON)

  implicit val rnd = new scala.util.Random(42)

/*
  def mtbfile: Action[AnyContent] =
    Action.async {
      implicit request =>

      Future.successful(
        Gen.of[MTBFile].next
      )
      .map {
        mtbf =>
//TODO: correct -- MediaType match doesn't work yet

         render {
           case AcceptsFHIR()  =>
             Ok(mtbf.mapTo[MTBFileBundle].toFHIRJson).as(FHIR_JSON)

           case Accepts.Json() =>
             Ok(toJson(mtbf))
         }
      }
    }
*/

  def mtbfile: Action[AnyContent] =
    Action.async {
      request =>

      Future.successful(
        Gen.of[MTBFile].next
      )
      .map {
        mtbf =>

         request.acceptedTypes
           .find(_.mediaSubType.startsWith("fhir"))
           .fold(
             Ok(toJson(mtbf))
           )(
             ct => Ok(mtbf.mapTo[MTBFileBundle].toFHIRJson).as(FHIR_JSON)
           )

      }
    }

}
