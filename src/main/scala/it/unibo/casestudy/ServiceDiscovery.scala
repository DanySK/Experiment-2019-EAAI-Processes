package it.unibo.casestudy

import it.unibo.alchemist.model.implementations.positions.Euclidean2DPosition
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.space.{Point2D, Point3D}
import Services._

class ServiceDiscovery extends AggregateProgram with StandardSensors with Gradients with BlockG with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with BlockS with BlockC {

  import Spawn._

  lazy val providedServices: Set[ProvidedService] =
    randomGenerator()
      .shuffle(services)
      .take(randomGenerator().nextInt(services.size))
      .map(s => ProvidedService(s)(numInstances = 1)
    )

  def task: Option[Task] =
    if(node.has("task"))
      Some(node.get[Task]("task"))
    else
      None

  override def main(): Any = {
    node.put("providedServices", providedServices)
    node.put("hasTask", !task.isEmpty)

    classicServiceDiscovery()
    processBasedServiceDiscovery()
  }

  def classicServiceDiscovery() = {

  }

  def processBasedServiceDiscovery() = {

  }

  // Other stuff

  override def currentPosition(): Point3D = {
    val pos = sense[Euclidean2DPosition](LSNS_POSITION)
    Point3D(pos.getX, pos.getY, 0)
  }

  def current2DPosition(): Point2D = Point2D(currentPosition().x, currentPosition().y)
}