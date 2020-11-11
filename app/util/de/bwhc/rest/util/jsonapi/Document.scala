package de.bwhc.rest.util.jsonapi


sealed trait Document


object DocumentMembers
{

  trait meta[T, C[_]]{
    this: Document =>
    val meta: C[T]
  }



}



