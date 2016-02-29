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
import com.utils.SamplingRateHistoryRequest
import scala.collection.mutable.ListBuffer

import SamplingRateProtocol._
case class SamplingRateHistoryHelper(samplingRateHistory: scala.collection.immutable.List[SamplingRateHelper])
object SamplingRateHistoryProtocol extends DefaultJsonProtocol {
  implicit val formatSampling = jsonFormat1(SamplingRateHistoryHelper)
}
import SamplingRateHistoryProtocol._
class SamplingRateHistoryActor extends Actor with SprayJsonSupport {
  def receive = {
    case SamplingRateHistoryRequest(r, registrationId, limit, db) => {
      val collection = db("SamplingRateHistory")
      val query = BSONDocument("registrationId" -> registrationId)
      val registrationIdList = collection.find(query).sort(BSONDocument("timestamp" -> -1)).
        cursor[BSONDocument].collect[List](limit)
      registrationIdList.onComplete {
        case Success(result) => {
          if (result.isEmpty)
            r.complete("Registration Id not found...")
          else {
            val samplingRateHistory = ListBuffer[SamplingRateHelper]()
            for (data <- result) {
              samplingRateHistory += SamplingRateHelper(data.getAs[String]("name").get,
                data.getAs[String]("registrationId").get, data.getAs[Date]("timestamp").get.toGMTString(),
                data.getAs[Double]("samplingRate").get, data.getAs[Int]("score").get,
                data.getAs[String]("description").get, data.getAs[String]("unit").get)
            }
            val dataList = samplingRateHistory.toList
            r.complete(marshal(SamplingRateHistoryHelper(dataList)))
          }
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

