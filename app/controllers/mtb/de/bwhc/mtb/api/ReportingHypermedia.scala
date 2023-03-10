package de.bwhc.mtb.api


import scala.concurrent.{ExecutionContext,Future}
import de.bwhc.auth.api.UserWithRoles
import de.bwhc.rest.util.sapphyre._
import de.bwhc.util.syntax.piping._
import de.bwhc.mtb.data.entry.dtos.{
  Medication
}
import de.bwhc.mtb.query.api.{
  ConceptCount
}


trait ReportingHypermedia
{

  import de.bwhc.rest.util.sapphyre.syntax._
  import de.bwhc.rest.util.sapphyre.Relations._
  import de.bwhc.rest.util.sapphyre.Method._


  private val BASE_URI       = "/bwhc/mtb/api/reporting"

  private val LOCAL_REPORT   = "local-qc-report"
  private val GLOBAL_REPORT  = "global-qc-report"

  private val MEDICATION_DISTRIBUTION   = "global-medication-distribution"
  private val TUMOR_ENTITY_DISTRIBUTION = "global-tumor-entity-distribution"
  private val PATIENT_THERAPIES         = "patient-therapies"


  private object Scope extends Enumeration
  {
    val Local  = Value("local")
    val Global = Value("global")
  }


  val ApiBaseLink =
    Link(s"$BASE_URI/")


  private def ReportLink(scope: Scope.Value) =
    Link(s"$BASE_URI/QCReport?scope=$scope")

  private val MedicationDistributionLink =
    Link(s"$BASE_URI/$MEDICATION_DISTRIBUTION")
/*
  private def TumorEntityDistributionLinkFor(code: String, version: String) =
    Link(s"$BASE_URI/$TUMOR_ENTITY_DISTRIBUTION?code=$code&version=$version")

  private def TumorEntityDistributionLink =
    TumorEntityDistributionLinkFor("{ATC-code}","{ATC-version}")


  private def PatientTherapiesLinkFor(code: String, version: String) =
    Link(s"$BASE_URI/$PATIENT_THERAPIES?code=$code&version=$version")

  private def PatientTherapiesLink =
    PatientTherapiesLinkFor("{ATC-code}","{ATC-version}")
*/

  private def TumorEntityDistributionLink =
    Link(s"$BASE_URI/$TUMOR_ENTITY_DISTRIBUTION[?code={ATC-code}&version={ATC-version}]")

  private def PatientTherapiesLink =
    Link(s"$BASE_URI/$PATIENT_THERAPIES[?code={ATC-code}&version={ATC-version}]")


  def ApiResource(
    implicit
    user: UserWithRoles,
    ec: ExecutionContext
  ) = {
    for {

      canGetLocalReport  <- user has QueryPermissions.LocalQCAccessRight

      canGetGlobalReport <- user has QueryPermissions.GlobalQCAccessRight

      api = Resource.empty.withLinks(SELF -> ApiBaseLink)

      result =
        api |
        (r => if (canGetLocalReport)
                r.withLinks(LOCAL_REPORT  -> ReportLink(Scope.Local))
              else r) |
        (r => if (canGetGlobalReport)
                r.withLinks(
                  GLOBAL_REPORT             -> ReportLink(Scope.Global),
                  MEDICATION_DISTRIBUTION   -> MedicationDistributionLink,
                  TUMOR_ENTITY_DISTRIBUTION -> TumorEntityDistributionLink,
                  PATIENT_THERAPIES         -> PatientTherapiesLink
                )
              else r)
    } yield result
  }

}
object ReportingHypermedia extends ReportingHypermedia


