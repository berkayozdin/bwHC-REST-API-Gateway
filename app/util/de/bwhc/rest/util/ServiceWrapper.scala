package de.bwhc.rest.util



import de.bwhc.util.spi._


abstract class ServiceWrapper[T <: ServiceProviderInterface]
(
  private val loader: SPILoader[T]
)
{
  val instance: T#Service = loader.getInstance.get
}
