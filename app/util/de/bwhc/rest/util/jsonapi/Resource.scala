package de.bwhc.rest.util.jsonapi


abstract class Resource


object Resource
{

  @annotation.implicitNotFound(
    "Couldn't find Type instance ${T}. Define one or ensure it is in scope"
  )
  sealed trait Type[T]{
    val name: String
  }

  object Type 
  {
    def apply[T](t: String): Type[T] = new Type[T]{ val name = t }

    def apply[T](implicit typ: Type[T]) = typ
  }

/*
  class Identifier[T](val id: String) extends Resource
  
  class MetaIdentifier[T, M](id: String, val meta: M) extends Identifier[T](id)
*/

//  case class Id(value: String) extends AnyVal


  trait id {
    this: Resource =>
    val id: String
  }


  trait meta[T,C[_]]{
    this: Document =>
    val meta: T
  }


  trait attributes[T <: Product]{
    this: Resource =>
    val attributes: T
  }



}

