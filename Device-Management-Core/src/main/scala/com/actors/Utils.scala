package com

package actors {
  object RegistrationIdQuery
  case class LastKnownValueQuery(registrationId: String, sensorId: String)
  case class SamplingRateQuery(registrationId: String, sensorId: String)
  case class DeviceStatusQuery(registrationId: String, sensorId: String)
  case class SensorIdQuery(registrationId: String)
  object StartWorkers
  object WorkerResults
  object WorkerResultsLastKnown
  object WorkerResultsSamplingRate
}