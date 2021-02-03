package de.bwhc.rest.util.sapphyre


import shapeless.{HNil,<:!<}

object syntax
{

  implicit class HypermediaOps[T](val t: T) extends AnyVal
  {

    def withMeta[M](m: M) =
      Resource(t).withMeta(m)
 
    def withLinks(ls: (String,Link)*) =
      Resource(t).withLinks(ls: _*)
 
    def withActions(as: (String,Action)*) =
      Resource(t).withActions(as: _*)
 
    def withEmbedded[R](rel: String, r: R)(implicit emb: R IsIn Embeddable) =
      Resource(t).withEmbedded(rel,r)

/*
    def asHyperResource[R <: Resource[T,_,_]](
      implicit hyper: Hyper[T,R]
    ): R = hyper(t)
*/

  }


}
