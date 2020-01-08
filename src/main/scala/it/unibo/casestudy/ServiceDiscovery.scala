package it.unibo.casestudy

import it.unibo.alchemist.model.implementations.positions.Euclidean2DPosition
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.space.{Point2D, Point3D}

class ServiceDiscovery extends AggregateProgram with StandardSensors with Gradients with BlockG with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with BlockS with BlockC {

  override def currentPosition(): Point3D = {
    val pos = sense[Euclidean2DPosition](LSNS_POSITION)
    Point3D(pos.getX, pos.getY, 0)
  }

  def current2DPosition(): Point2D = Point2D(currentPosition().x, currentPosition().y)

  import Spawn._

  trait Service
  case object  Database  extends Service
  case object OCR extends Service
  case object  SpeechRecogniser extends Service
  val services: Set[Service] = Set(Database, OCR, SpeechRecogniser)

  trait IProvidedService {
    val service: Service
    val numInstances: Int
  }
  case class ProvidedService(service: Service)(val numInstances: Int = 1) extends IProvidedService

  case class Task(requiredServices: Set[Service])

  lazy val providedServices: Set[ProvidedService] =
    randomGenerator().shuffle(services).take(randomGenerator().nextInt(services.size+1)).map(
      s => ProvidedService(s)(numInstances = 1)
    )

  var task: Option[Task] = None

  override def main(): Any = {
    generateTask()

    node.put("providedServices", providedServices)
    node.put("hasTask", !task.isEmpty)
    node.put("task", task)

  }

  private def generateTask(): Unit = if(task.isEmpty) {
    if(randomGenerator().nextGaussian()>1){
      task = Some(Task((randomGenerator().shuffle(services)-providedServices).take(1+randomGenerator().nextInt(3))))
    }
  }
}