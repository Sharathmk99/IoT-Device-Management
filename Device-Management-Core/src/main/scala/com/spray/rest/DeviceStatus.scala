package com.spray.rest

import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._
import akka.actor.Actor
import com.utils.RegistrationRequest
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.json._
import DefaultJsonProtocol._
import spray.util._
import spray.httpx.SprayJsonSupport._
import spray.httpx.SprayJsonSupport
import reactivemongo.bson.BSONDocument
import scala.util.Success
import scala.util.Failure
import com.utils.DeviceStatusRequest

import java.util.Date

case class DeviceStatusHelper(name: String, registrationId: String, timestamp: String, status: String)
object DeviceStatusProtocol extends DefaultJsonProtocol {
  implicit val format = jsonFormat4(DeviceStatusHelper)
}
import DeviceStatusProtocol._
class DeviceStatusActor extends Actor with SprayJsonSupport {
  def receive = {
    case DeviceStatusRequest(r, registrationId, db) => {
      val collection = db("DeviceStatus")
      val query = BSONDocument("registrationId" -> registrationId)
      val projection = BSONDocument("name" -> 1)
      val registrationIdList = collection.find(query).one[BSONDocument]

      registrationIdList.onComplete {
        case Success(result) => {
          //registrationDummy.map(println)
          if (result.isEmpty)
            r.complete("Registration Id not found...")
          else
            r.complete(marshal(DeviceStatusHelper(result.get.getAs[String]("name").get,
              result.get.getAs[String]("registrationId").get, result.get.getAs[Date]("timestamp").get.toGMTString(),
              result.get.getAs[String]("status").get)))
          context.stop(self)
        }
        case Failure(failure) => {
          r.complete("Something went wrong...")
          context.stop(self)
        }
      }

    }
  }
}

