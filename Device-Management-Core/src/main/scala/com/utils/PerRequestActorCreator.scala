package com.utils

import akka.actor.Actor
import spray.routing.RequestContext
import akka.actor.ActorRef
import akka.actor.Props
import com.spray.rest.RegistrationIdActor
import com.spray.rest.RegistrationIDs
import spray.routing.RequestContext
import reactivemongo.api.DefaultDB
import reactivemongo.api.DefaultDB
import com.spray.rest.DeviceStatusActor
import com.spray.rest.LastKnownDataActor
import com.spray.rest.SamplingRateActor
import com.spray.rest.SamplingRateHistoryActor

//case class WithActorRef(r: RequestContext, target: ActorRef) extends RegistrationIdActor

trait PerRequestActorCreator {
  this: Actor =>

  def registrationRequest(r: RequestContext, connection: DefaultDB) =
    context.actorOf(Props[RegistrationIdActor], "spray-registration") ! RegistrationRequest(r, connection)

  def deviceStatusRequest(r: RequestContext, registrationId: String, connection: DefaultDB) =
    context.actorOf(Props[DeviceStatusActor], "spray-devicestatus") ! DeviceStatusRequest(r, registrationId, connection)

  def lastKnownDataRequest(r: RequestContext, registrationId: String, entry: Int, connection: DefaultDB) =
    context.actorOf(Props[LastKnownDataActor], "spray-lastknowndata") ! LastKnownDataRequest(r, registrationId, entry, connection)

  def samplingRateRequest(r: RequestContext, registrationId: String, connection: DefaultDB) =
    context.actorOf(Props[SamplingRateActor], "spray-samplingrate") ! SamplingRateRequest(r, registrationId, connection)

  def samplingRateHistoryRequest(r: RequestContext, registrationId: String, limit: Int, connection: DefaultDB) =
    context.actorOf(Props[SamplingRateHistoryActor], "spray-samplingratehistory") !
      SamplingRateHistoryRequest(r, registrationId, limit, connection)
}
case class RegistrationRequest(r: RequestContext, connection: DefaultDB)
case class DeviceStatusRequest(r: RequestContext, data: String, connection: DefaultDB)
case class LastKnownDataRequest(r: RequestContext, registrationId: String, entry: Int, connection: DefaultDB)
case class SamplingRateRequest(r: RequestContext, data: String, connection: DefaultDB)
case class SamplingRateHistoryRequest(r: RequestContext, registrationId: String, limit: Int, connection: DefaultDB)