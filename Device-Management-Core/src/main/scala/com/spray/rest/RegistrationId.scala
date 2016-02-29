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

case class RegistrationIDs(registrationIds: scala.collection.immutable.List[String])
object RegistrationIdProtocol extends DefaultJsonProtocol {
  implicit val format = jsonFormat1(RegistrationIDs)
}
import RegistrationIdProtocol._
class RegistrationIdActor extends Actor with SprayJsonSupport {
  def receive = {
    case RegistrationRequest(r, db) => {
      val registrationDummy: scala.collection.mutable.ListBuffer[String] = new scala.collection.mutable.ListBuffer[String]()
      val collection = db("DeviceStatus")
      val query = BSONDocument()
      val projection = BSONDocument("registrationId" -> 1)
      val registrationIdList = collection.find(query, BSONDocument("registrationId" -> 1))
        .cursor[BSONDocument].collect[List]()

      registrationIdList.onComplete {
        case Success(result) => {

          for (registrationId <- result) {
            registrationDummy += (registrationId.getAs[String]("registrationId").get)
          }
          //registrationDummy.map(println)
          val registrationIds = registrationDummy.toList
          r.complete(marshal(RegistrationIDs(registrationIds)))
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

