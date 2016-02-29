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

import com.utils.LastKnownDataRequest

case class LastKnownDataHelper(name: String, registrationId: String, timestamp: String, value: Double)
object LastKnownDataHelperProtocol extends DefaultJsonProtocol {
  implicit val format1 = jsonFormat4(LastKnownDataHelper)
}
import LastKnownDataHelperProtocol._
case class LastKnowDataMarshall(list:scala.collection.immutable.List[LastKnownDataHelper])
object LastKnownDataProtocol extends DefaultJsonProtocol {
  implicit val format = jsonFormat1(LastKnowDataMarshall)
}
import LastKnownDataProtocol._
class LastKnownDataActor extends Actor with SprayJsonSupport {
  def receive = {
    case LastKnownDataRequest(r, registrationId, limit, db) => {
      val collection = db("SensorData")
      val query = BSONDocument("registrationId" -> registrationId)
      val lastKnownDataList = collection.find(query).sort(BSONDocument("timestamp" -> -1))
        .cursor[BSONDocument].collect[List](limit)
      lastKnownDataList.onComplete {
        case Success(result) => {
          if (result.isEmpty) {
            r.complete("Registration Id or Data not found...")
          } else {
            val lastKnownDataDummy: scala.collection.mutable.ListBuffer[LastKnownDataHelper] =
              new scala.collection.mutable.ListBuffer[LastKnownDataHelper]()
            for (data <- result) {
              val lastknowndata = LastKnownDataHelper(data.getAs[String]("name").get,
                data.getAs[String]("registrationId").get, data.getAs[Date]("timestamp").get.toGMTString(),
                data.getAs[Double]("value").get)
                lastKnownDataDummy += lastknowndata
            }
            val lastKnownDataList = lastKnownDataDummy.toList
            r.complete(marshal(LastKnowDataMarshall(lastKnownDataList)))
          }
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

