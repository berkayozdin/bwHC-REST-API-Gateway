package de.bwhc.mtb.rest.api



import play.api.libs.json._


case class SearchSet[T]
(
  entries: Iterable[T]
)
{
  val total: Int = entries.size
}


object SearchSet
{

  implicit def format[T: Format]: Format[SearchSet[T]] = 
    Format[SearchSet[T]](
      Reads(
        js =>
          (js \ "entries").validate[Iterable[T]].map(SearchSet(_))
      ),
      Writes(
        s => 
          Json.obj(
            "entries" -> Json.toJson(s.entries),
            "total"   -> s.total
          )
      )
    )

}
