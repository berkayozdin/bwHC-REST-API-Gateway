package de.bwhc.rest.util.sapphyre


import shapeless.HList


import cats.Id


object Hyper
{

  def apply[T, R, Meta, Embedded <: HList, F[_]](
    t: T
  )(
    implicit
    f: T => F[Resource[R,Meta,Embedded]]
  ) = f(t)


}


/*
sealed trait Hyper[F[_]]
{
  def apply[T, R, Meta, Embedded <: HList](
    t: T
  )(
    implicit
    f: T => F[Resource[R,Meta,Embedded]]
  ) = f(t)

}

object Hyper
{
  def apply[F[_]]: Hyper[F] = new Hyper[F]{}
}
*/
