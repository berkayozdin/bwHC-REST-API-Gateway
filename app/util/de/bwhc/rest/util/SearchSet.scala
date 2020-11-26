package de.bwhc.rest.util



import play.api.libs.json._


case class SearchSet[T]
(
  entries: Iterable[T]
)
{
  lazy val total: Int = entries.size
}


object SearchSet
{

  implicit def writes[T: Writes]: Writes[SearchSet[T]] = 
    Writes(
      s => 
        Json.obj(
          "entries" -> Json.toJson(s.entries),
          "total"   -> s.total
        )
    )

/*
  implicit def format[T: Format]: Format[SearchSet[T]] = 
    Format[SearchSet[T]](
      Reads(
        js => (js \ "entries").validate[Iterable[T]].map(SearchSet(_))
      ),
      Writes(
        s => 
          Json.obj(
            "entries" -> Json.toJson(s.entries),
            "total"   -> s.total
          )
      )
    )
*/

}
