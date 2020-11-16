package de.bwhc.rest.util


import shapeless.{
  Coproduct, :+:, Inl, Inr, CNil, DepFn1
}


package object scapphyre
{


@annotation.implicitNotFound("${T} is not in ${C}")
trait IsIn[T, C <: Coproduct] extends DepFn1[C]{ type Out = Boolean }
object IsIn
{

  def apply[T, C <: Coproduct](implicit isIn: IsIn[T,C]): IsIn[T,C] = isIn

  implicit def isInH[H, S <: H, T <: Coproduct]: IsIn[S,H :+: T] =
    new IsIn[S,H :+: T]{
      def apply(c: H :+: T) =
        c match {
          case Inl(h) => true
          case Inr(t) => false
        }
    }

  implicit def isInT[H, T <: Coproduct, U](
    implicit isIn: IsIn[U,T]
  ): IsIn[U,H :+: T] =
    new IsIn[U,H :+: T]{
      def apply(c: H :+: T) =
        c match {
          case Inl(h) => false
          case Inr(t) => isIn(t)
        }
    }

}

  type ∈ [T, C <: Coproduct] = IsIn[T,C]


  type Embeddable =
    Link :+:
    Resource[_,_,_] :+:
    Iterable[Link] :+:
    Iterable[Resource[_,_,_]] :+:
    CNil



}
