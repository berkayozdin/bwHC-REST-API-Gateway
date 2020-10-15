package de.bwhc.mtb.api


import javax.inject.Singleton


import de.bwhc.rest.util.ServiceWrapper

import de.bwhc.mtb.data.entry.api.MTBDataService
import de.bwhc.mtb.query.api.QueryService


@Singleton
class WrappedDataService extends ServiceWrapper(MTBDataService)


@Singleton
class WrappedQueryService extends ServiceWrapper(QueryService)



/*
@Singleton
class Services
{

  val dataService = MTBDataService.getInstance.get

  val queryService = QueryService.getInstance.get

}
*/
