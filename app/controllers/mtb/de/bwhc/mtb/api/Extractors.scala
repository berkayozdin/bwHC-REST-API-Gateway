package de.bwhc.mtb.api


import de.bwhc.mtb.data.entry.dtos.Patient
import de.bwhc.mtb.query.api.Query


object QueryId
{
  def unapply(s: String): Option[Query.Id] = 
    Some(Query.Id(s))
}

object PatId
{
  def unapply(s: String): Option[Patient.Id] = 
    Some(Patient.Id(s))
}


