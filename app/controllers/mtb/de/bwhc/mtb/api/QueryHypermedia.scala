package de.bwhc.mtb.api


import scala.concurrent.{ExecutionContext,Future}

import play.api.libs.json.JsValue

import de.bwhc.util.json.schema._
import de.bwhc.rest.util.sapphyre._

import de.bwhc.auth.api.UserWithRoles

import de.bwhc.util.syntax.piping._

import de.bwhc.mtb.query.api.{
  PatientView,
  Query,
  QueryOps,
  NGSSummary,
  ResultSummary
}
import de.bwhc.mtb.data.entry.dtos.{
  MTBFile,
  Patient
}
import de.bwhc.mtb.data.entry.views.{
  MTBFileView,
  TherapyRecommendationView,
  MolecularTherapyView
}




trait QueryHypermedia
{

  import syntax._
  import Method._
  import Relations._
  import QuerySchemas._
  import QueryPermissions._


  private val BASE_URI = "/bwhc/mtb/api/query"

  private val SUBMIT_LOCAL_QUERY     = "submit-local-query"
  private val SUBMIT_FEDERATED_QUERY = "submit-federated-query"
  private val APPLY_FILTER           = "apply-filter"

  private val QUERY           = "query"
  private val RESULT_SUMMARY  = "result-summary"
  private val PATIENTS        = "patients"
  private val NGS_SUMMARIES   = "ngs-summaries"
  private val RECOMMENDATIONS = "therapy-recommendations"
  private val THERAPIES       = "molecular-therapies"
  private val MTBFILE         = "mtbfile"
  private val MTBFILEVIEW     = "mtbfileView"



  val ApiBaseLink =
    Link(s"$BASE_URI/")


  private val LocalQueryAction =
    SUBMIT_LOCAL_QUERY -> Action(POST -> BASE_URI)
                            .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$QUERY"))

  private val FederatedQueryAction =
    SUBMIT_FEDERATED_QUERY -> Action(POST -> BASE_URI)
                                .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$QUERY"))

  private def UpdateAction(queryId: Query.Id) =
    UPDATE -> Action(POST -> s"$BASE_URI/${queryId.value}")
                .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$UPDATE"))

  private def ApplyFilterAction(queryId: Query.Id) =
    APPLY_FILTER -> Action(POST -> s"$BASE_URI/${queryId.value}/filter")
                      .withFormats(MediaType.APPLICATION_JSON -> Link(s"$BASE_URI/schema/$APPLY_FILTER"))


  private def QueryLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}")

  private def ResultSummaryLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/result-summary")

  private def PatientsLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/patients")

  private def NGSSummariesLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/ngs-summaries")

  private def RecommendationsLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/therapy-recommendations")

  private def TherapiesLink(queryId: Query.Id) =
    Link(s"$BASE_URI/${queryId.value}/molecular-therapies")

  private def MTBFileLink(queryId: Query.Id, patId: Patient.Id) = 
    Link(s"$BASE_URI/${queryId.value}/mtbfiles/${patId.value}")

  private def MTBFileViewLink(queryId: Query.Id, patId: Patient.Id) = 
    Link(s"$BASE_URI/${queryId.value}/mtbfileViews/${patId.value}")



  private val schemas =
    Map(
      QUERY        -> JsValueSchema[QueryForm],
      UPDATE       -> JsValueSchema[QueryOps.Command.Update],
      APPLY_FILTER -> JsValueSchema[QueryOps.Command.ApplyFilter],
      PATIENTS     -> JsValueSchema[PatientView],
    )


  def schema(rel: String): Option[JsValue] =
    schemas.get(rel) 


  def Api(
    implicit
    user: UserWithRoles,
    ec: ExecutionContext
  ) = {
    for {

      localQueryRights     <- user has LocalEvidenceQueryRight

      federatedQueryRights <- user has FederatedEvidenceQueryRight

      result =
        Resource.empty.withLinks(SELF -> ApiBaseLink) |
        (r => if (localQueryRights)     r.withActions(LocalQueryAction)     else r) |
        (r => if (federatedQueryRights) r.withActions(FederatedQueryAction) else r)

    } yield result
  }


  def HyperQuery(
    query: Query
  )(
    implicit
    user: UserWithRoles,
    ec: ExecutionContext
  ) = { 
    for {
      mtbFileAccess <- user has MTBFileAccessRight

      result = 
        query.withLinks(
          BASE            -> ApiBaseLink,
          SELF            -> QueryLink(query.id),
          RESULT_SUMMARY  -> ResultSummaryLink(query.id)
        )
        .withActions(
          UpdateAction(query.id),
          ApplyFilterAction(query.id)
        ) | (
          q =>
          if (mtbFileAccess)
            q.withLinks(
              PATIENTS        -> PatientsLink(query.id),
              NGS_SUMMARIES   -> NGSSummariesLink(query.id),
              RECOMMENDATIONS -> RecommendationsLink(query.id),
              THERAPIES       -> TherapiesLink(query.id)
            )
          else q 
        )
    } yield result

  }

/*
  def HyperQuery(
    query: Query
  ) = {
    query.withLinks(
      BASE            -> ApiBaseLink,
      SELF            -> QueryLink(query.id),
      PATIENTS        -> PatientsLink(query.id),
      NGS_SUMMARIES   -> NGSSummariesLink(query.id),
      RECOMMENDATIONS -> RecommendationsLink(query.id),
      THERAPIES       -> TherapiesLink(query.id)
    )
    .withActions(
      UpdateAction(query.id),
      ApplyFilterAction(query.id)
    )

  }
*/

  def HyperResultSummary(
    result: ResultSummary
  )(
    queryId: Query.Id
  ) = {
    result.withLinks(
      BASE       -> ApiBaseLink,
      QUERY      -> QueryLink(queryId)
    )
  }


  def HyperPatient(
    patient: PatientView
  )(
    queryId: Query.Id
  ) = {
    patient.withLinks(
      BASE       -> ApiBaseLink,
      QUERY      -> QueryLink(queryId),
      COLLECTION -> PatientsLink(queryId),
      MTBFILE    -> MTBFileLink(queryId,patient.id),
      MTBFILEVIEW -> MTBFileViewLink(queryId,patient.id)
    )

  }


  def HyperMTBFile(
    mtbfile: MTBFile
  )(
    queryId: Query.Id
  ) = {
    mtbfile.withLinks(
      BASE  -> ApiBaseLink,
      SELF  -> MTBFileLink(queryId,mtbfile.patient.id),
      QUERY -> QueryLink(queryId)
    )
  }


  def HyperMTBFileView(
    mtbfile: MTBFileView
  )(
    queryId: Query.Id
  ) = {
    mtbfile.withLinks(
      BASE  -> ApiBaseLink,
      SELF  -> MTBFileLink(queryId,mtbfile.patient.id),
      QUERY -> QueryLink(queryId)
    )
  }


  def HyperNGSSummary(
    ngs: NGSSummary
  )(
    queryId: Query.Id
  ) = {
    ngs.withLinks(
      BASE        -> ApiBaseLink,
      COLLECTION  -> NGSSummariesLink(queryId),
      MTBFILE     -> MTBFileLink(queryId,ngs.patient),
      MTBFILEVIEW -> MTBFileViewLink(queryId,ngs.patient),
      QUERY       -> QueryLink(queryId)
    )
  }


  def HyperTherapyRecommendation(
    th: TherapyRecommendationView
  )(
    queryId: Query.Id
  ) = {
    th.withLinks(
      BASE        -> ApiBaseLink,
      COLLECTION  -> RecommendationsLink(queryId),
      MTBFILE     -> MTBFileLink(queryId,th.patient),
      MTBFILEVIEW -> MTBFileViewLink(queryId,th.patient),
      QUERY       -> QueryLink(queryId)
    )
  }

}
object QueryHypermedia extends QueryHypermedia


