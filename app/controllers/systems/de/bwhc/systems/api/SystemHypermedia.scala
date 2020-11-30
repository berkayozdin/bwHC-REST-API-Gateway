package de.bwhc.systems.api


import de.bwhc.rest.util.sapphyre._


import de.bwhc.mtb.data.entry.dtos.{Patient,MTBFile}


import json._
import com.github.andyglow.jsonschema.CatsSupport._

import de.bwhc.util.json.schema._


trait JsonSchemas
{
  implicit val mtbFileSchema  = Json.schema[MTBFile]
}
object JsonSchemas extends JsonSchemas



trait SystemHypermedia
{

  import Relations._
  import Method._
  import MediaType._

  import JsonSchemas._


  private val BASE_URI = "/bwhc/system/api"

  
  val ApiBaseLink =
    Link(s"$BASE_URI/")


  private val UploadMTBFile =
    "upload-mtbfile" -> Action(POST -> s"$BASE_URI/data/upload")  //TODO: format
                          .withFormats(APPLICATION_JSON -> Link(s"$BASE_URI/schema/upload-mtbfile"))

  private val DeletePatient =
    "delete-patient" -> Action(Method.DELETE -> s"$BASE_URI/data/Patient/{id}")

  private val PeerToPeerQuery =
    "peer2peer-query" -> Action(POST -> s"$BASE_URI/peer2peer/query")

  private val PeerToPeerLocalQCReport =
    "peer2peer-local-report" -> Action(GET -> s"$BASE_URI/peer2peer/LocalQCReport")


  private val schemas =
    Map(
      UploadMTBFile._1 -> JsValueSchema[MTBFile]
    )

  def schemaFor(rel: String) = //: Option[JsValue] =
    schemas.get(rel)



  val ApiResource =
    Resource.empty
      .withLinks(SELF -> ApiBaseLink)
      .withActions(
        UploadMTBFile,
        DeletePatient,
        PeerToPeerQuery,
        PeerToPeerLocalQCReport
      )


}
object SystemHypermedia extends SystemHypermedia
