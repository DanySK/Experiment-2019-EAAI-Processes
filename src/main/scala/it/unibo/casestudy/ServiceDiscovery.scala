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

  var allocatedServices: Set[ProvidedService] = Set.empty

  def availableServices: Set[ProvidedService] = providedServices.filter(_.freeInstances>0)

  def task: Option[Task] =
    if(node.has("task"))
      Some(node.get[Task]("task"))
    else
      None

  override def main(): Any = {
    node.put("providedServices", providedServices)
    node.put("hasTask", !task.isEmpty)

    if(node.get[Boolean]("algorithm")){
      processBasedServiceDiscovery()
    } else {
      classicServiceDiscovery()
    }

  }

  def classicServiceDiscovery() = {
    val hasTask = !task.isEmpty
    val g = classicGradient(hasTask)
    node.put("gradient", g)

    val taskRequest = rep[Option[TaskRequest]](None) { tr =>
        val localTaskRequest = tr.orElse(task.map(t => TaskRequest(mid, t)(allocation = Map.empty)))
        val receivedRequest = bcast(hasTask, localTaskRequest)
        node.put("receivedRequest", receivedRequest.isDefined)
        node.put("request", receivedRequest)

        val offeredService: Option[Service] = receivedRequest.flatMap { request =>
          request.allocation.find(_._2 == mid).map(_._1) // keep current allocation
            .orElse[Service] { // or offer an available service
              request.missingServices.collectFirst { case s if availableServices.find(_.service == s).isDefined => s }
            }
        }
        node.put("offersService", offeredService.isDefined)
        node.put("offeredService", offeredService)

        allocatedServices = Set.empty
        offeredService.foreach { s =>
          allocatedServices += ProvidedService(s)()
        }

        val offers = C[Double, Map[ID, Service]](g, _ ++ _, offeredService.map(s => Map(mid -> s)).getOrElse(Map.empty), Map.empty)
        localTaskRequest.map(t => t.copy()(allocation = chooseFromOffers(t, offers)))
    }

    val keepFor: Long = branch(taskRequest.isDefined && taskRequest.get.missingServices.isEmpty){
      val start = rep(timestamp())(x => x)
      timestamp() - start
    }{ 0 }
    if(keepFor > 10 && node.has("task")){
      node.remove("task")
    }
  }

  def chooseFromOffers(req: TaskRequest, offers: Map[ID,Service]): Map[Service,ID] = {
    var servicesToAlloc = req.missingServices
    var newAllocations = Map[Service,ID]()
    for(offer <- offers){
      if(servicesToAlloc.contains(offer._2)){
        newAllocations += offer.swap
        servicesToAlloc -= offer._2
      }
    }

    // keep current allocations if still provided, and add new allocations
    req.allocation.filter(curr => offers.contains(curr._2)) ++ newAllocations
  }

  def processBasedServiceDiscovery() = {

  }

  // Other stuff

  def bcast[V](source: Boolean, field: V): V =
    G[V](source, field, v => v, nbrRange _)

  override def currentPosition(): Point3D = {
    val pos = sense[Euclidean2DPosition](LSNS_POSITION)
    Point3D(pos.getX, pos.getY, 0)
  }

  def current2DPosition(): Point2D = Point2D(currentPosition().x, currentPosition().y)
}