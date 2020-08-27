package de.bwhc.catalogs.api



import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._




class Router @Inject()(
  catalogs: CatalogsController,
)
extends SimpleRouter
{

  override def routes: Routes = {

    //-------------------------------------------------------------------------
    // Catalogs / ValueSets
    //-------------------------------------------------------------------------
    case GET(p"/Coding/$system"?q_o"pattern=$pattern")            => catalogs.coding(system,pattern)
    case GET(p"/Coding"?q"system=$system"?q_o"pattern=$pattern")  => catalogs.coding(system,pattern)


    case GET(p"/ValueSet")                                        => catalogs.valueSets
    case GET(p"/ValueSet/$name")                                  => catalogs.valueSet(name)
    case GET(p"/ValueSet"?q"name=$name")                          => catalogs.valueSet(name)

//    case GET(p"/ValueSet"?q_o"language=$lang")          => catalogs.valueSets(lang)
//    case GET(p"/ValueSet/$name"?q_o"language=$lang")    => catalogs.valueSet(name,lang)

  }


}
