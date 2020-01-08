package it.unibo.casestudy

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.ID

trait Service

case object Database extends Service

case object OCR extends Service

case object SpeechRecogniser extends Service

trait IProvidedService {
  val service: Service
  val numInstances: Int
}

case class ProvidedService(service: Service)(val numInstances: Int = 1) extends IProvidedService

case class Task(requiredServices: Set[Service])

case class TaskRequest(requestor: ID, task: Task)

object Services {
  val services: Set[Service] = Set(Database, OCR, SpeechRecogniser)
}