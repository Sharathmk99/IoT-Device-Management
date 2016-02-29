package com.actors

import java.util.Date

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import akka.actor.Actor
import akka.actor.actorRef2Scala
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.Producer.nameValue2Producer

class LastKnownValueActor(connection: DefaultDB) extends Actor {

  def receive = {
    case LastKnownValueQuery(registrationId, sensorName) =>
      //println("got inside Last known value actor " + sensorName)
      getlastKnowValue(connection, registrationId, sensorName)
  }

  def getlastKnowValue(db: DefaultDB, registrationId: String, sensorName: String) = {
    val collection = db("SensorData")
    val query = BSONDocument("name" -> sensorName, "registrationId" -> registrationId)
    val lastKnowData = collection.find(query, BSONDocument("value" -> 1, "timestamp" -> 1))
      .sort(BSONDocument("timestamp" -> -1)).one[BSONDocument]
    lastKnowData.map { x =>
      {
        //println(sensorName)
        //println(s"found document: ${BSONDocument pretty x.get}")
        //writeDoc(db, registrationId, (x.get.getAs[Double]("value")).get, (x.get.getAs[Date]("timestamp")).get, sensorName)
        updateDoc(db, registrationId, (x.get.getAs[Double]("value")).get, (x.get.getAs[Date]("timestamp")).get, sensorName)
      }
    }
    sender ! WorkerResultsLastKnown
  }

  def writeDoc(db: DefaultDB, registrationId: String, value: Double, timestamp: Date, sensorName: String) = {
    val collection = db("LastKnownValueHistory")
    val document = BSONDocument("registrationId" -> registrationId,
      "value" -> value, "timestamp" -> timestamp, "name" -> sensorName)
    val future1: Future[WriteResult] = collection.insert(document)
    future1.onComplete {
      case Failure(e)           => throw e
      case Success(writeResult) =>
      //println(s"successfully inserted document with result: $writeResult")
    }
  }

  def updateDoc(db: DefaultDB, registrationId: String, value: Double, timestamp: Date, sensorName: String) = {
    val collection = db("LastKnowData")
    val selector = BSONDocument("registrationId" -> registrationId, "name" -> sensorName)
    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "value" -> value,
        "timestamp" -> timestamp))
    val future1: Future[WriteResult] = collection.update(selector, modifier, upsert = true)
    future1.onComplete {
      case Failure(e)           => throw e
      case Success(writeResult) =>
      //println(s"successfully inserted document with result: $writeResult")
    }
  }
}