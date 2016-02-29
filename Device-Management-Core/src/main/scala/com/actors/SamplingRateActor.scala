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
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONString
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.core.commands.RawCommand

class SamplingRateActor(connection: DefaultDB) extends Actor {
  var timestamp = 0;
  def receive = {
    case SamplingRateQuery(registrationId, sensorId) => mapReduce(registrationId, sensorId)
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
    result.map { x =>
      {
        //println(BSONDocument.pretty(x))
        val resultArray = x.getAs[BSONArray]("results")
        var samplingRate = 0.0;
        var score = 0;
        var description = ""
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
                              samplingRate = (valueList.get(0).get / (1000 * 60))
                              score = 100
                              description = ("Sampling rate is " + valueList.get(0).get / (1000 * 60) + "min registrationId "
                                + registrationId + " sensor Name " + sensorId + " score at 100%")
                            } else {
                              var nonMap: scala.collection.mutable.Map[Double, Int] = scala.collection.mutable.Map()
                              valueList.distinct.foreach { x =>
                                {
                                  //println(valueList.count { _ == x } + " " + x)
                                  nonMap += x -> (valueList.count { _ == x })
                                }
                              }
                              val max = nonMap.maxBy(_._2)
                              samplingRate = (max._1 / (1000 * 60))
                              score = ((max._2 / 10.0) * 100).toInt;
                              description = ("Sampling rate is " + max._1 / (1000 * 60) + "min registrationId "
                                + registrationId + " sensor Name " + sensorId + " score at " + ((max._2 / valueList.size.toDouble) * 100) + "%")
                            }
                          }
                        }
                      }
                  }
              }
            }

          }
        }
        updateDoc(connection, registrationId, new Date(), sensorId, samplingRate, score, description, "min")
        writeDoc(connection, registrationId, new Date(), sensorId, samplingRate, score, description, "min")
      }
    }
    sender ! WorkerResultsSamplingRate
  }

  def writeDoc(db: DefaultDB, registrationId: String, timestamp: Date, sensorName: String, samplingRate: Double, score: Int,
               description: String, unit: String) = {
    val collection = db("SamplingRateHistory")
    val document = BSONDocument("registrationId" -> registrationId,
      "timestamp" -> timestamp, "name" -> sensorName, "samplingRate" -> samplingRate,
      "score" -> score, "description" -> description, "unit" -> unit)
    val future1: Future[WriteResult] = collection.insert(document)
    future1.onComplete {
      case Failure(e)           => throw e
      case Success(writeResult) =>
      //println(s"successfully inserted document with result: $writeResult")
    }
  }

  def updateDoc(db: DefaultDB, registrationId: String, timestamp: Date, sensorName: String, samplingRate: Double, score: Int,
                description: String, unit: String) = {
    val collection = db("SamplingRate")
    val selector = BSONDocument("registrationId" -> registrationId, "name" -> sensorName)
    val modifier = BSONDocument(
      "$set" -> BSONDocument("timestamp" -> timestamp, "samplingRate" -> samplingRate,
        "score" -> score, "description" -> description, "unit" -> unit))
    val future1: Future[WriteResult] = collection.update(selector, modifier, upsert = true)
    future1.onComplete {
      case Failure(e)           => throw e
      case Success(writeResult) =>
      //println(s"successfully inserted document with result: $writeResult")
    }
  }
}