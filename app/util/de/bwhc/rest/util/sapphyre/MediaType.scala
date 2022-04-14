package de.bwhc.rest.util.sapphyre



case class MediaType(value: String) extends AnyVal

object MediaType
{

  val APPLICATION_JSON             = MediaType("application/json")
  val JSON                         = APPLICATION_JSON

  val JSON_SCHEMA                  = MediaType("application/schema+json")


  val FORM_URL_ENCODED             = MediaType("application/x-www-form-urlencoded")
  val APPLICATION_FORM_URL_ENCODED = FORM_URL_ENCODED

  //TODO: other MIME Types

}

