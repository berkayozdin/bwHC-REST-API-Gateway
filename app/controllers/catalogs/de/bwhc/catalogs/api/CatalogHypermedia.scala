package de.bwhc.catalogs.api



/*
import de.bwhc.rest.util.sapphyre
import de.bwhc.rest.util.sapphyre._
import de.bwhc.rest.util.sapphyre.json._

import play.api.libs.json._
import play.api.libs.json.Json.toJson


trait CatalogHypermedia
{

  import sapphyre.Method._
  import sapphyre.MediaType._
  import sapphyre.Relations._


  val baseUrl = "/bwhc/catalogs/api"

  val baseEntity =
    toJson(
    Entity
      .withLinks(
        Link(Self,baseUrl).withType(Hypermedia_JSON)
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
object CatalogHypermedia extends CatalogHypermedia
*/


import de.bwhc.rest.util.sapphyre
import de.bwhc.rest.util.sapphyre._
import de.bwhc.rest.util.sapphyre.playjson._


trait CatalogHypermedia
{

  import sapphyre.Relations._


  private val BASE_URI = "/bwhc/catalogs/api"

  private val catalogLinks =
    List(
       "icd-10-gm",
       "icd-o-3-t",
       "icd-o-3-m",
       "hgnc",
       "atc"
     )
     .map(sys => sys -> Link(s"$BASE_URI/Coding?system=$sys"))


  private val valueSetLinks =
        Catalogs
          .jsonValueSets
          .keys
          .map(vs => vs -> Link(s"$BASE_URI/ValueSet?name=$vs"))
          .toList       

  val ApiResource =
    Resource.empty
      .withLinks(
        SELF -> Link(s"$BASE_URI/")
      )
      .withLinks(catalogLinks: _*)
      .withLinks("valuesets" -> Link(s"$BASE_URI/ValueSet"))
      .withLinks(valueSetLinks: _*)

}
object CatalogHypermedia extends CatalogHypermedia



