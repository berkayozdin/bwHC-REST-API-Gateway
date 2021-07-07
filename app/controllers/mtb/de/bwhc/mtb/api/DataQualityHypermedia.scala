package de.bwhc.mtb.api


import scala.concurrent.{ExecutionContext,Future}

import de.bwhc.rest.util.Table
import de.bwhc.rest.util.sapphyre._

import de.bwhc.mtb.data.entry.dtos.{Patient,MTBFile}
import de.bwhc.mtb.data.entry.api.{DataQualityReport,PatientDataInfo}

import de.bwhc.auth.api.UserWithRoles


trait DataQualityHypermedia
{

  import de.bwhc.rest.util.sapphyre.syntax._
  import de.bwhc.rest.util.sapphyre.Relations._


  private val BASE_URI = "/bwhc/mtb/api/data-quality"

  private val PATIENTS            = "patients"
  private val MTBFILE             = "mtbfile"
  private val DATA_QUALITY_REPORT = "data-quality-report"

        
  val ApiBaseLink =
    Link(s"$BASE_URI/")

  private val PatientsLink =
    Link(s"$BASE_URI/$PATIENTS")

  private def MTBFileLink(id: Patient.Id) =
    Link(s"$BASE_URI/$PATIENTS/${id.value}/$MTBFILE")
     
  private def DataQualityReportLink(id: Patient.Id) =
    Link(s"$BASE_URI/$PATIENTS/${id.value}/$DATA_QUALITY_REPORT")
     


  val ApiResource =
    Resource.empty
      .withLinks(
        SELF     -> ApiBaseLink,
        PATIENTS -> PatientsLink
      )


  def HyperPatient(
    pat: PatientDataInfo
  ) = {
    pat.withLinks(
      COLLECTION          -> PatientsLink,
      MTBFILE             -> MTBFileLink(pat.id),
      DATA_QUALITY_REPORT -> DataQualityReportLink(pat.id)
    )
  }

  def HyperMTBFile(
    mtbfile: MTBFile
  ) = 
    mtbfile.withLinks(
      SELF                -> MTBFileLink(mtbfile.patient.id),
      DATA_QUALITY_REPORT -> DataQualityReportLink(mtbfile.patient.id),
      PATIENTS            -> PatientsLink
    )


  def HyperDataQualityReport(
    report: DataQualityReport
  ) = 
    report.withLinks(
      SELF     -> DataQualityReportLink(report.patient), 
      MTBFILE  -> MTBFileLink(report.patient),
      PATIENTS -> PatientsLink
    )

  
  
  implicit val patientHeader =
    Table.Header[PatientDataInfo](
      "id"             -> "ID",
      "birthDate"      -> "Birthdate",
      "gender"         -> "Gender",
      "numberOfIssues" -> "Number of Issues"
    )



  def HyperPatients[C[X] <: Iterable[X]](
    pats: C[PatientDataInfo]
  ) =
    Table(pats.map(HyperPatient))
      .withLinks(
        BASE -> ApiBaseLink,
        SELF -> PatientsLink
      )



}
object DataQualityHypermedia extends DataQualityHypermedia


