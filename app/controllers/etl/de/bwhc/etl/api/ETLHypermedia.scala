package de.bwhc.etl.api


import de.bwhc.rest.util.sapphyre._
import de.bwhc.mtb.dtos.{Patient,MTBFile}
import json._
import com.github.andyglow.jsonschema.CatsSupport._
import de.bwhc.util.json.schema._


trait JsonSchemas
{
  implicit val mtbFileSchema  = Json.schema[MTBFile]
}
object JsonSchemas extends JsonSchemas



trait ETLHypermedia
{

  import Relations._
  import Method._
  import MediaType._

  import JsonSchemas._


  private val BASE_URI = "/bwhc/etl/api"

  
  val ApiBaseLink =
    Link(s"$BASE_URI/")


  private val UploadMTBFile =
    "upload-mtbfile" -> Action(POST -> s"$BASE_URI/MTBFile")
                          .withFormats(APPLICATION_JSON -> Link(s"$BASE_URI/schema/upload-mtbfile"))

  private val DeletePatient =
    "delete-patient-data" -> Action(Method.DELETE -> s"$BASE_URI/Patient/{id}")


  private val schemas =
    Map(
      UploadMTBFile._1 -> JsValueSchema[MTBFile]
    )

  def schemaFor(rel: String) =
    schemas.get(rel)



  val ApiResource =
    Resource.empty
      .withLinks(SELF -> ApiBaseLink)
      .withActions(
        UploadMTBFile,
        DeletePatient
      )


}
object ETLHypermedia extends ETLHypermedia
