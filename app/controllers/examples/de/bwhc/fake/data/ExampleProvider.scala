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
  Result
}
import play.api.libs.json.Json.toJson

import de.ekut.tbi.generators.Gen

import de.bwhc.mtb.data.entry.dtos.MTBFile
import de.bwhc.mtb.data.gens._


import de.bwhc.util.mapping.syntax._
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


  implicit val rnd = new scala.util.Random(42)



/*
  def mtbfile: Action[AnyContent] =
    Action.async {
      Future.successful(
        Gen.of[MTBFile].next
      )
      .map(toJson(_))
      .map(Ok(_))
    }
*/


  def mtbfile: Action[AnyContent] =
    Action.async {
      implicit request =>

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

/*
          if (request.accepts(FHIR_JSON))
            Ok(mtbf.mapTo[MTBFileBundle].toFHIRJson).as(FHIR_JSON)
          else
            Ok(toJson(mtbf))
*/
      }
    }


  def fhirMtbfile: Action[AnyContent] =
    Action.async {
      Future.successful(
        Gen.of[MTBFile].next
      )
      .map(
        _.mapTo[MTBFileBundle].toFHIRJson
      )
      .map(
        Ok(_).as(FHIR_JSON)
      )
    }

}
