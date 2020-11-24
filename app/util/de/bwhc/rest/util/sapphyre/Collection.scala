package de.bwhc.rest.util.sapphyre



import shapeless.{
  HList, HNil, ::
}


object Collection
{

  case class Properties private (total: Int)

  object Properties
  {
    def apply[T, C[X] <: Iterable[X]](ts: C[T]): Properties =
      Properties(ts.size)
  }


/*
  def apply[T, C[X] <: Iterable[X]](
    rel: String, ts: C[T]
  )(
    implicit emb: C[T] IsIn Embeddable
  ): Resource[Collection.Properties, HNil, (String,C[T]) :: HNil] =
    Resource(Properties(ts))
      .withEmbedded(rel,ts)
*/


  def apply[T, C[X] <: Iterable[X]](
    ts: C[T]
  )(
    implicit emb: C[T] IsIn Embeddable
  ): Resource[Collection.Properties, HNil, (String,C[T]) :: HNil] =
    Resource(Properties(ts))
      .withEmbedded("items",ts)
//    apply("items",ts)


}
