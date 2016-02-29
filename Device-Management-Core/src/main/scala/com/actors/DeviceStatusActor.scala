package com.actors

import java.util.Date

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import akka.actor.Actor
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONString
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.core.commands.RawCommand

class DeviceStatusActor(connection: DefaultDB) extends Actor {
  def receive = {
    case DeviceStatusQuery(registrationId, sensorId) => mapReduce(registrationId, sensorId)
  }

  def mapReduce(registrationId: String, sensorId: String) = {

    val map = "function(){emit({'name' : this.name, 'registrationId' : this.registrationId},this.timestamp);}"
    val reduce = "function(key,value){var result = {value:[]};var temp = [];" +
      "for(var i=0;i<(value.length-1);i++){temp.push((value[i]) - (value[i+1]));" +
      "}result.value = temp;return result;}"
    val mapReduceCommand = BSONDocument("mapreduce" -> "SensorData",
      "map" -> BSONString(map),
      "reduce" -> BSONString(reduce),
      "out" -> BSONDocument("inline" -> 1),
      "limit" -> 10,
      "sort" -> BSONDocument("timestamp" -> -1),
      "query" -> BSONDocument("registrationId" -> registrationId, "name" -> sensorId))
    val result = connection.command(RawCommand(mapReduceCommand))
    var samplingRate = 0.0;
    result.map { x =>
      {
        //println(BSONDocument.pretty(x))
        val resultArray = x.getAs[BSONArray]("results")
        resultArray.foreach { x =>
          {
            val data = x.getAs[BSONDocument](0)
            data.foreach { result =>
              {
                result.getAs[BSONDocument]("value")
                  .foreach {
                    x =>
                      {
                        val values = x.getAs[BSONArray]("value")
                        values.map { xx =>
                          {
                            val resultValues = xx.iterator
                            var valueList: scala.collection.mutable.MutableList[Double] = scala.collection.mutable.MutableList()
                            while (resultValues.hasNext) {
                              try {
                                val temp = resultValues.next()
                                valueList += (temp.get._2).asInstanceOf[BSONDouble].value
                                //println(resultValues.next().get._2)
                              } catch {
                                case t: Throwable => t.printStackTrace() // TODO: handle error
                              }
                            }
                            if (valueList.distinct.size == 1) {
                              samplingRate = (valueList.get(0).get)
                              findStatus(samplingRate, registrationId, sensorId)
                            } else {
                              var nonMap: scala.collection.mutable.Map[Double, Int] = scala.collection.mutable.Map()
                              valueList.distinct.foreach { x =>
                                {
                                  nonMap += x -> (valueList.count { _ == x })
                                }
                              }
                              val max = nonMap.maxBy(_._2)
                              samplingRate = (max._1)
                              findStatus(samplingRate, registrationId, sensorId)
                            }
                          }
                        }
                      }
                  }
              }
            }
          }
        }
      }
    }
  }

  def findStatus(samplingRate: Double, registrationId: String, sensorId: String) = {
    val collections = connection("SensorData")
    val query = BSONDocument("registrationId" -> registrationId, "name" -> sensorId)
    val timestamp = collections.find(query, BSONDocument("timestamp" -> 1))
      .sort(BSONDocument("timestamp" -> -1)).one[BSONDocument]
    timestamp.map { time =>
      {
        try {
          val times = (time.get.getAs[Date]("timestamp")).get
          val currentTime = new Date
          var result = (currentTime.getTime - times.getTime)
          if (result < samplingRate) {
            writeDoc(connection, registrationId, new Date, sensorId, "Alive");
            updateDoc(connection, registrationId, new Date, sensorId, "Alive");
          } else {
            writeDoc(connection, registrationId, new Date, sensorId, "Dead");
            updateDoc(connection, registrationId, new Date, sensorId, "Dead");
          }
        } catch {
          case t: Throwable => t.printStackTrace() // TODO: handle error
        }

      }
    }
  }

  def writeDoc(db: DefaultDB, registrationId: String, timestamp: Date, sensorName: String, status: String) = {
    val collection = db("DeviceStatusHistory")
    val document = BSONDocument("registrationId" -> registrationId,
      "timestamp" -> timestamp, "name" -> sensorName, "status" -> status)
    val future1: Future[WriteResult] = collection.insert(document)
    future1.onComplete {
      case Failure(e)           => throw e
      case Success(writeResult) =>
      //println(s"successfully inserted document with result: $writeResult")
    }
  }

  def updateDoc(db: DefaultDB, registrationId: String, timestamp: Date, sensorName: String, status: String) = {
    val collection = db("DeviceStatus")
    val selector = BSONDocument("registrationId" -> registrationId, "name" -> sensorName)
    val modifier = BSONDocument(
      "$set" -> BSONDocument("timestamp" -> timestamp, "status" -> status))
    val future1: Future[WriteResult] = collection.update(selector, modifier, upsert = true)
    future1.onComplete {
      case Failure(e)           => throw e
      case Success(writeResult) =>
      //println(s"successfully inserted document with result: $writeResult")
    }
  }
}