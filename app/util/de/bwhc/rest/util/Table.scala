package de.bwhc.rest.util



import shapeless.{
  HList, HNil, ::,
  Lazy, <:!<
}

import play.api.libs.json._



case class Table[T] private (
  header: Seq[Table.ColumnMapping],
  entries: Seq[T]
)
{
  lazy val total = entries.size
}



object Table
{

  import de.bwhc.rest.util.sapphyre.Resource


  case class ColumnMapping
  (
    value: String,
    text: String
  )

  sealed abstract class Header[T](val mappings: Seq[ColumnMapping])

  object Header 
  {

    def apply[T](
      implicit
      nr: T <:!< Resource[T,_,_],
      ht: Header[T]
    ) = ht

    implicit def apply[T, R <: Resource[T,_,_]](
      implicit
      ht: Header[T]
    ): Header[R] =
      ht.asInstanceOf[Header[R]]


//    def apply[T](ms: (Symbol,String)*): Header[T] =
    def apply[T](ms: (String,String)*): Header[T] =
      new Header[T](
        ms.toSeq.map { case (f,c) => ColumnMapping(f,c) }
      ){}

  }


  implicit val formatColumnMapping = Json.format[ColumnMapping]
  implicit def format[T: Writes]: Writes[Table[T]] =
    Writes(
      table => 
        Json.obj(
          "header"  -> Json.toJson(table.header),
          "entries" -> Json.toJson(table.entries),
          "total"   -> table.total
        )
    )



  def apply[T, C[X] <: Iterable[X]](
    ts: C[T]
  )(
    implicit
    header: Header[T]
  ): Table[T] =
    Table(
      header.mappings,
      ts.toSeq
    )


}
