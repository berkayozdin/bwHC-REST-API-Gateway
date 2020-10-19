package de.bwhc.services


import javax.inject.Singleton


import de.bwhc.rest.util.ServiceWrapper

import de.bwhc.mtb.query.api.QueryService



@Singleton
class WrappedQueryService extends ServiceWrapper(QueryService)


