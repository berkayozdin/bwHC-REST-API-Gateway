package de.bwhc.rest.util.sapphyre



import shapeless.{
  HList, HNil, ::,
  Lazy, <:!<
}
import shapeless.labelled.FieldType


object Table
{

  case class ColumnMapping
  (
    value: String,
//    field: Symbol,
    text: String
  )


  sealed abstract class Header[T](val mappings: Seq[ColumnMapping])

/*
  sealed abstract class Header[T, Ms <: HList]
  (
    val mappings: Ms
  )
*/

  object Header 
  {

    class Builder[T](private val dummy: Boolean) extends AnyVal {

      def apply[Ms <: HList](
        mappings: Ms
//      ): Header[T,Ms] = ??? 
      ): Header[T] = ??? 

    }  


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


    def apply[T](ms: (String,String)*): Header[T] =
//    def apply[T](ms: (Symbol,String)*): Header[T] =
      new Header[T](
        ms.toSeq.map { case (f,c) => ColumnMapping(f,c) }
      ){}

  }


  case class Meta private (
    header: Seq[ColumnMapping]
  )


  def apply[T, C[X] <: Iterable[X]](
    ts: C[T]
  )(
    implicit
    emb: C[T] IsIn Embeddable,
    header: Header[T]
  ) =
    Collection(ts)
      .withMeta(Table.Meta(header.mappings))



}
