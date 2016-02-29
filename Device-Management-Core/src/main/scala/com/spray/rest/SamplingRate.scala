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

import com.utils.SamplingRateRequest

case class SamplingRateHelper(name: String, registrationId: String, timestamp: String, samplingRate: Double,
                              score: Int, description: String, unit: String)
object SamplingRateProtocol extends DefaultJsonProtocol {
  implicit val format = jsonFormat7(SamplingRateHelper)
}
import SamplingRateProtocol._
class SamplingRateActor extends Actor with SprayJsonSupport {
  def receive = {
    case SamplingRateRequest(r, registrationId, db) => {
      val collection = db("SamplingRate")
      val query = BSONDocument("registrationId" -> registrationId)
      val registrationIdList = collection.find(query).one[BSONDocument]
      registrationIdList.onComplete {
        case Success(result) => {
          if (result.isEmpty)
            r.complete("Registration Id not found...")
          else
            r.complete(marshal(SamplingRateHelper(result.get.getAs[String]("name").get,
              result.get.getAs[String]("registrationId").get, result.get.getAs[Date]("timestamp").get.toGMTString(),
              result.get.getAs[Double]("samplingRate").get, result.get.getAs[Int]("score").get,
              result.get.getAs[String]("description").get, result.get.getAs[String]("unit").get)))
          context.stop(self)
        }
        case Failure(failure) => {
          r.complete("Something went wrong...")
          context.stop(self)
        }
      }
    } case _ => {
      println("Something went wrong...")
      context.stop(self)
    }

  }
}

