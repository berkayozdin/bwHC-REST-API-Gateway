package de.bwhc.rest.util.sapphyre


import shapeless.{
  HList, HNil, ::
}


import cats.data.NonEmptyList


sealed trait Document extends Linked


case class Resource[T, Meta, Embedded <: HList]
(
  data: T,
  meta: Option[Meta] = None,
  links: Map[String,Link] = Map.empty[String,Link],
  actions: Map[String,Action] = Map.empty[String,Action],
  embedded: Embedded
)
extends Document
{

  def withData[U](u: U) =
    copy(data = u)

  def withMeta[M](m: M) =
    copy(meta = Some(m))

  def withLinks(ls: (String,Link)*) =
    copy(links = links ++ ls)

  def withActions(as: (String,Action)*) =
    copy(actions = actions ++ as)

  def withEmbedded[R](rel: String, r: R)(implicit emb: R IsIn Embeddable) =
    copy(embedded = (rel -> r) :: embedded)

}


object Resource
{

  def apply[T](t: T): Resource[T,HNil,HNil] =
    Resource(
      data = t,
      embedded = HNil
   )

  val empty: Resource[HNil,HNil,HNil] =
    Resource(
      data = HNil,
      embedded = HNil
    )

}


case class ErrorReport[E]
(
  errors: NonEmptyList[E],
  links: Map[String,Link] = Map.empty[String,Link]
)
extends Document

