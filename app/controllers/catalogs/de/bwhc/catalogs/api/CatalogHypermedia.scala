package de.bwhc.catalogs.api



import de.bwhc.rest.util.sapphyre
import de.bwhc.rest.util.sapphyre._
import de.bwhc.rest.util.sapphyre.playjson._


trait CatalogHypermedia
{

  import sapphyre.Relations._


  private val BASE_URI = "/bwhc/catalogs/api"

  private val catalogLinks =
    List(
      "ICD-10-GM",
      "ICD-O-3-T",
      "ICD-O-3-M",
      "HGNC",
      "ATC"
    )
    .map(_.toLowerCase)
    .map(sys => sys -> Link(s"$BASE_URI/Coding?system=$sys"))


  private val valueSetLinks =
    Catalogs
      .jsonValueSets
      .keys
      .map(vs => vs -> Link(s"$BASE_URI/ValueSet?name=$vs"))
      .toList       

  val ApiBaseLink =
    Link(s"$BASE_URI/")

  val ApiResource =
    Resource.empty
      .withLinks(
        SELF -> ApiBaseLink
      )
      .withLinks(catalogLinks: _*)
      .withLinks("valuesets" -> Link(s"$BASE_URI/ValueSet"))
      .withLinks(valueSetLinks: _*)

}
object CatalogHypermedia extends CatalogHypermedia



