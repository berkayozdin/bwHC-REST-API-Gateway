package de.bwhc.mtb.api


import de.bwhc.mtb.dtos.{
  Patient,
  Medication
}
import de.bwhc.mtb.query.api.{
  PreparedQuery,
  Query
}


abstract class Extractor[S,T](
  f: S => T
)
{
  final def unapply(s: S): Option[T] =
    Some(f(s))
}

abstract class OptExtractor[S,T](
  f: S => T
)
{
  final def unapply(s: Option[S]): Option[Option[T]] =
    Some(s map f)
}


object QueryId extends Extractor(Query.Id(_))

object PreparedQueryId extends Extractor(PreparedQuery.Id(_))

object PatId extends Extractor(Patient.Id(_))

object MedicationCode extends OptExtractor(Medication.Code(_))

/*

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
*/
