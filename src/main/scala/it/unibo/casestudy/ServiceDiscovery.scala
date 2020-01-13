package it.unibo.casestudy

import it.unibo.alchemist.model.implementations.positions.Euclidean2DPosition
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.space.{Point2D, Point3D}
import Services._

class ServiceDiscovery extends AggregateProgram with StandardSensors with Gradients with BlockG with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with BlockS with BlockC {

  import Spawn._

  lazy val providedServices: Set[ProvidedService] = {
    if(alchemistRandomGen.nextDouble() < 0.25)
    randomGenerator()
      .shuffle(services)
      .take(randomGenerator().nextInt(services.size))
      .map(s => ProvidedService(s)(numInstances = 1)
    )
    else Set.empty
  }

  var allocatedServices: Set[ProvidedService] = Set.empty

  def availableServices: Set[ProvidedService] =
    providedServices.filter(_.freeInstances>0) -- offeredServices.keys

  def task: Option[Task] =
    if(node.has("task"))
      Some(node.get[Task]("task"))
    else
      None

  override def main(): Any = {
    node.put("providedServices", providedServices)
    node.put("hasTask", task.isDefined)
    node.put("t_time", currentTime())
    node.put("t_timestamp", timestamp())

    if(node.get[Boolean]("algorithm")){
      processBasedServiceDiscovery()
    } else {
      classicServiceDiscovery()
    }
  }

  def offeredServices: Map[ProvidedService,TaskRequest] = if(node.has("offeredServices")){
    node.get[Map[ProvidedService,TaskRequest]]("offeredServices")
  } else {
    node.put("offeredServices", Map.empty)
    Map.empty
  }

  def addOfferedService(tr: TaskRequest, ns: ProvidedService) = {
    node.put("offeredServices", offeredServices + (ns -> tr))
  }

  def classicServiceDiscovery() = {
    val hasTask = task.isDefined
    val g = classicGradient(hasTask)
    val gHops = G[Int](hasTask, 0, _+1, nbrRange _)
    node.put("gradient", f"$g%2.1f")

    var taskChanged = false
    var hops: Map[ID,Int] = Map.empty

    val taskRequest = rep[Option[TaskRequest]](None) { tr =>
        val theTask = task.map(t => TaskRequest(mid, t)(allocation = Map.empty))
        taskChanged = theTask != tr
        val localTaskRequest = if(tr==theTask) tr else theTask
        val receivedRequest = bcast(hasTask, localTaskRequest)
        node.put("receivedRequest", receivedRequest.isDefined)
        node.put("request", receivedRequest)
        if(receivedRequest.isEmpty && node.has("requestBy")) node.remove("requestBy")
        else if(receivedRequest.isDefined) node.put("requestBy", receivedRequest.get.requestor%20)

        val offeredService: Option[Service] = receivedRequest.flatMap { request =>
          request.allocation.find(_._2 == mid).map(_._1) // keep current allocation
            .orElse[Service] { // or offer an available service
              request.missingServices.collectFirst { case s if availableServices.exists(_.service == s) => s }
            }
        }
        node.put("offersService", offeredService.isDefined)
        node.put("offeredService", offeredService)

        allocatedServices = Set.empty
        offeredService.foreach { s =>
          allocatedServices += ProvidedService(s)()
        }

        val offers = C[Double, Map[ID, Service]](g, _ ++ _, offeredService.map(s => Map(mid -> s)).getOrElse(Map.empty), Map.empty)
        hops = C[Double,Map[ID,Int]](g, _++_, offeredService.map(s => Map(mid -> gHops)).getOrElse(Map.empty), Map.empty)
        localTaskRequest.map(t => t.copy()(allocation = chooseFromOffers(t, offers)))
    }

    // Time a device is trying to satisfy a task
    val tryFor: Long = branch(!taskChanged && taskRequest.exists(_.missingServices.nonEmpty) ){
      val start = rep(timestamp())(x => x)
      timestamp() - start
    }{ 0 }
    // Time a device has a task satisfied
    val keepFor: Long = branch(taskRequest.isDefined && taskRequest.get.missingServices.isEmpty){
      val start = rep(timestamp())(x => x)
      timestamp() - start
    }{ 0 }
    // Condition for task removal (accomplishment)
    val accomplished = keepFor > node.get[Number]("taskPropagationTime").longValue
    val giveUp = tryFor > node.get[Number]("taskConclusionTime").longValue
    if((accomplished || giveUp) && node.has("task")){
      node.remove("task")
      val latency: Int = (timestamp()-node.get[Long]("taskTime")).toInt
      node.put("taskLatency", if(node.has("taskLatency")) node.get[Int]("taskLatency")+latency else latency)
      val numHops: Int = taskRequest.get.allocation.values.map(hops(_)).sum
      val cloudHops: Int = taskRequest.get.missingServices.map(_ => node.get[Int]("cloudcost")).sum
      val totalHops = numHops + cloudHops
      node.put("taskHops", if(node.has("taskHops")) node.get[Number]("taskHops").intValue() + totalHops else totalHops)
      if(accomplished) {
        node.put("completedTasks", if(node.has("completedTasks")) node.get[Int]("completedTasks")+1 else 1)
      }
      if(giveUp) {
        node.put("giveupTasks", if(node.has("giveupTasks")) node.get[Int]("giveupTasks")+1 else 1)
      }
    }
  }

  def chooseFromOffers(req: TaskRequest, offers: Map[ID,Service]): Map[Service,ID] = {
    var servicesToAlloc = req.missingServices
    var newAllocations = Map[Service,ID]()
    for(offer <- offers.toList.sortBy(_._1)){
      if(servicesToAlloc.contains(offer._2)){
        newAllocations += offer.swap
        servicesToAlloc -= offer._2
      }
    }

    // keep current allocations if still provided, and add new allocations
    req.allocation.filter(curr => offers.contains(curr._2)) ++ newAllocations
  }

  def processBasedServiceDiscovery() = {
    val theTask = task.map(t => TaskRequest(mid, t)(allocation = Map.empty)).toSet

    val procs = sspawn[TaskRequest, Unit, TaskRequest](serviceDiscoveryProcess, theTask, ())
    node.put("procs", procs)
    node.put("numProcs", procs.size)
    node.put("numProcsOthers", procs.count(_._2.requestor!=mid))
    node.put("numProcsMine", procs.count(_._2.requestor==mid))

    procs.filter(_._2.requestor==mid)
  }

  def serviceDiscoveryProcess(taskRequest: TaskRequest)(args: Unit): (TaskRequest, Status) = {
    val source = taskRequest.requestor==mid
    val gHops = hopGradient(source)
    val g = classicGradient(source)

    var continueExpansion = true
    var hops: Map[ID,Int] = Map.empty

    val pid = s"proc_${taskRequest.hashCode()}_"

    node.put(pid+"hops", gHops)

    case class State(currentDistance: Int = 0, taskRequest: TaskRequest = taskRequest)
    val s = rep(State()){ oldState =>
      val receivedRequest = bcast(source, oldState)
      continueExpansion = receivedRequest.currentDistance>=gHops-1 && !receivedRequest.taskRequest.missingServices.isEmpty

      node.put(pid+"receivedRequest", receivedRequest)
      node.put(pid+"request", receivedRequest)
      node.put(pid+"requestBy", receivedRequest.taskRequest.requestor%20)

      val servicesToOffer: Set[Service] =
        (offeredServices.filter(_._2 == receivedRequest.taskRequest).keys ++ receivedRequest.taskRequest.missingServices.flatMap(s => {
          val newServiceToOffer = availableServices.find(_.service == s).toSet
          newServiceToOffer.foreach(ns => addOfferedService(receivedRequest.taskRequest, ns))
          newServiceToOffer
        })).map(_.service).toSet

      node.put("numOfferedServices", offeredServices.size)

      val offers = C[Double, Map[ID, Service]](gHops, _ ++ _, servicesToOffer.map(s => mid -> s).toMap, Map.empty)
      hops = C[Double,Map[ID,Int]](gHops, _++_, servicesToOffer.map(s => mid -> gHops).toMap, Map.empty)
      val maxExt = C[Double,Int](g, Math.max(_,_), gHops, -1)
      val newTaskRequest = taskRequest.copy()(allocation = chooseFromOffers(taskRequest, offers))

      State(maxExt, newTaskRequest)
    }

    node.put(pid+"state", s)

    // Time a device is trying to satisfy a task
    val tryFor: Long = branch(taskRequest.missingServices.nonEmpty){
      val start = rep(timestamp())(x => x)
      timestamp() - start
    }{ 0 }
    // Time a device has a task satisfied
    val keepFor: Long = branch(taskRequest.missingServices.isEmpty){
      val start = rep(timestamp())(x => x)
      timestamp() - start
    }{ 0 }
    // Condition for task removal (accomplishment)
    val accomplished = keepFor > node.get[Number]("taskPropagationTime").longValue
    val giveUp = tryFor > node.get[Number]("taskConclusionTime").longValue
    val done = accomplished || giveUp
    if(done && node.has("task") && node.get("task")==taskRequest.task){
      node.remove("task")
      val latency: Int = (timestamp()-node.get[Long]("taskTime")).toInt
      node.put("taskLatency", if(node.has("taskLatency")) node.get[Int]("taskLatency")+latency else latency)
      val numHops: Int = taskRequest.allocation.values.map(hops(_)).sum
      val cloudHops: Int = taskRequest.missingServices.map(_ => node.get[Int]("cloudcost")).sum
      val totalHops = numHops + cloudHops
      node.put("taskHops", if(node.has("taskHops")) node.get[Number]("taskHops").intValue() + totalHops else totalHops)
      if(accomplished) {
        node.put("completedTasks", if(node.has("completedTasks")) node.get[Int]("completedTasks")+1 else 1)
      }
      if(giveUp) {
        node.put("giveupTasks", if(node.has("giveupTasks")) node.get[Int]("giveupTasks")+1 else 1)
      }
    }

    val res = s match {
      case s if source && done => (s.taskRequest, Terminated)
      case s if source => (s.taskRequest, Output)
      case s => (s.taskRequest, if(continueExpansion) Output else External)
    }

    node.put(pid+"output", res)

    res
  }

  // Other stuff

  def hopGradient(src: Boolean): Int =
    classicGradient(src, () => 1).toInt

  def bcast[V](source: Boolean, field: V): V =
    G[V](source, field, v => v, nbrRange _)

  override def currentPosition(): Point3D = {
    val pos = sense[Euclidean2DPosition](LSNS_POSITION)
    Point3D(pos.getX, pos.getY, 0)
  }

  def current2DPosition(): Point2D = Point2D(currentPosition().x, currentPosition().y)
}