package de.bwhc.catalogs.api


import de.bwhc.rest.util.siren
import de.bwhc.rest.util.siren._
import de.bwhc.rest.util.siren.json._

import play.api.libs.json._
import play.api.libs.json.Json.toJson


trait CatalogSIREN
{

  import siren.Method._
  import siren.MediaType._
  import siren.Relations._


  val baseUrl = "/bwhc/catalogs/api"

  val baseEntity =
    toJson(
    Entity
      .withLinks(
        Link(Self,baseUrl).withType(SIREN_JSON)
      )
      .withEntities(
        List(
          "icd-10-gm",
          "icd-o-3-t",
          "icd-o-3-m",
          "hgnc",
          "atc"
        )
        .map(sys => EntityLink[JsObject](s"catalog-$sys", s"$baseUrl/Coding?system=$sys"))
      )
      .withEntities(

        EntityLink[JsObject]("valuesets", s"$baseUrl/ValueSet") +:

        Catalogs.jsonValueSets.keys
            .map(vs => EntityLink[JsObject](s"valueset-$vs",s"$baseUrl/ValueSet?name=$vs")).toList
        
      )
    )

}
object CatalogSIREN extends CatalogSIREN






