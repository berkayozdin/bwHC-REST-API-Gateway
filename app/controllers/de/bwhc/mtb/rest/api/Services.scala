package de.bwhc.mtb.rest.api


import javax.inject.Singleton

import de.bwhc.mtb.data.entry.api.MTBDataService
import de.bwhc.mtb.query.api.QueryService


@Singleton
class Services
{

  val dataService = MTBDataService.getInstance.get

  val queryService = QueryService.getInstance.get

}
