package com.http.service

import spray.routing.HttpService
import akka.actor.Actor
import spray.json._
import DefaultJsonProtocol._
import spray.util._
import spray.httpx.SprayJsonSupport._
import spray.http._
import MediaTypes._
import scala.language.postfixOps
import spray.json._
import spray.httpx.SprayJsonSupport
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.routing.RequestContext
import akka.actor.Props
import akka.actor.ActorRef
import com.utils.PerRequestActorCreator
import reactivemongo.api.DefaultDB

class HttpServiceActor(connection: DefaultDB) extends HttpService with Actor with SprayJsonSupport with PerRequestActorCreator {
  implicit def actorRefFactory = context
  def receive = runRoute(registrationId ~ deviceStatus ~ lastKnownData ~ samplingRate ~ samplingRateHistory)
  val registrationId = path("getRegistrationIds") {
    get {
      respondWithMediaType(`application/json`) {
        ctx => registrationRequest(ctx, connection)
      }
    }
  }
  val deviceStatus = path("getDeviceStatus" / Segment) { data =>
    get {
      respondWithMediaType(`application/json`) {
        ctx => deviceStatusRequest(ctx, data, connection)
      }
    }
  }
  val lastKnownData = path("getData" / Segment / IntNumber) { (data1, data2) =>
    get {
      respondWithMediaType(`application/json`) {
        ctx => lastKnownDataRequest(ctx, data1, data2, connection)
      }
    }
  }
  val samplingRate = path("getSamplingRate" / Segment) { data1 =>
    get {
      respondWithMediaType(`application/json`) {
        ctx => samplingRateRequest(ctx, data1, connection)
      }
    }
  }
  val samplingRateHistory = path("getSamplingRateHistory" / Segment / IntNumber) { (data1, data2) =>
    get {
      respondWithMediaType(`application/json`) {
        ctx => samplingRateHistoryRequest(ctx, data1, data2, connection)
      }
    }
  }
}

