package com.actors

import akka.actor.{ Props, Actor }
import scala.concurrent.duration._
import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONString
import akka.actor.ActorSelection.toScala
import akka.actor.actorRef2Scala
import reactivemongo.bson.Producer.nameValue2Producer

class SensorsIdActor(connection: DefaultDB) extends Actor {
  var totalSensor = 0
  var resultCountLastKnown = 0
  var resultCountSamplingRate = 0
  def receive = {
    case SensorIdQuery(registrationId) =>
      //println("Inside sensorId Actor " + registrationId)
      sensorId(connection, registrationId)
    case WorkerResultsLastKnown =>
      resultCountLastKnown += 1
      checkResultStatus
    case WorkerResultsSamplingRate =>
      resultCountSamplingRate += 1
      checkResultStatus
    case default =>
      println("Something else SensorIdActor $default" + default)
  }

  def checkResultStatus() {
    if (totalSensor == resultCountLastKnown && totalSensor == resultCountSamplingRate) {
      context.system.actorSelection("/user/registrationIdQuery") ! WorkerResults
    }
  }

  def sensorId(db: DefaultDB, registrationId: String) = {
    val collection = db("SensorData")
    val query = BSONDocument("registrationId" -> registrationId)
    val op: Option[BSONDocument] = Some(query)
    val sensorNames = collection.distinct("name", op)
    sensorNames.map { x =>
      {
        totalSensor = x.size
        //println(totalSensor + " Total Sensor")
        x.foreach { sensorName =>
          {
            context.system.actorOf(Props(new LastKnownValueActor(db))) ! LastKnownValueQuery(registrationId, sensorName.asInstanceOf[BSONString].value)
            context.system.actorOf(Props(new SamplingRateActor(db))) ! SamplingRateQuery(registrationId, sensorName.asInstanceOf[BSONString].value)
            context.system.actorOf(Props(new DeviceStatusActor(db))) ! DeviceStatusQuery(registrationId, sensorName.asInstanceOf[BSONString].value)
          }
        }
      }
    }
  }
}