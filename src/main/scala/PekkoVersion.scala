import PekkoVersion.Message.GreetOk
import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}

import java.nio.file.{Path, Paths}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

/**
 * PekkoVersion demonstrates the Actor Model for concurrent, message-driven programming using Apache Pekko.
 * It splits the sequential logic of DefaultVersion into independent actors that communicate via messages.
 *
 * **Key Differences from DefaultVersion:**
 *  - **Actor-Based Concurrency**: Three separate actors (PekkoSystem, ReadWriteActor, UpdateActor) instead of one sequential flow
 *  - **Message Passing**: Actors communicate via immutable messages, not direct method calls
 *  - **Asynchronous Coordination**: Operations happen independently; coordinator waits for responses
 *  - **State Isolation**: Each actor maintains its own private state
 *  - **Event-Driven**: Logic triggered by receiving messages, not sequential execution
 *
 * **What is the Actor Model?**
 * The Actor Model is a concurrency paradigm where:
 *  - **Actors** are independent units of computation with private state
 *  - **Messages** are the only way actors communicate (no shared memory)
 *  - **Mailboxes** queue incoming messages for each actor
 *  - **Asynchronous** message sending (fire-and-forget, non-blocking)
 *  - **Location transparency**: actors can be local or distributed
 *
 * **Architecture Overview:**
 * {{{
 * PekkoSystem (Coordinator)
 *     |
 *     +---> ReadWriteActor (handles file I/O)
 *     |
 *     +---> UpdateActor (handles user input)
 * 
 * Communication flow:
 * 1. main() creates PekkoSystem, sends Start message
 * 2. PekkoSystem creates ReadWriteActor and UpdateActor
 * 3. PekkoSystem sends Greet to both actors
 * 4. Actors respond with GreetOk
 * 5. PekkoSystem requests ReadRequest and UpdateRequest
 * 6. Actors process and respond with ReadAnswer and UpdateAnswer
 * 7. PekkoSystem coordinates the update when both responses received
 * 8. PekkoSystem sends WriteRequest to ReadWriteActor
 * 9. ReadWriteActor responds with WriteOk
 * 10. PekkoSystem sends GoodBye to actors and schedules Shutdown
 * }}}
 *
 *
 * **Key Scala Features Demonstrated:**
 *  - **Enums**: Message protocol defined with Scala 3 enum
 *  - **Pattern Matching**: onMessage methods use exhaustive pattern matching
 *  - **Mutable State**: Each actor has private mutable state (isolated, thread-safe)
 *
 * **Why Use the Actor Model?**
 *  - **Concurrency without threads**: Actors handle messages sequentially, eliminating race conditions
 *  - **Scalability**: Can easily add more actors or distribute across machines
 *  - **Resilience**: Actor supervision can restart failed actors
 *  - **Decoupling**: Actors know nothing about each other except message protocols
 *
 * **Trade-offs:**
 *  - **Complexity**: Much more complex than sequential code
 *  - **Debugging**: Harder to trace execution flow across actors
 *  - **Message overhead**: Creating and sending messages has cost
 *  - **Coordination**: Must explicitly handle asynchronous responses
 *
 * **When to Use Actor Model?**
 *  - **Highly concurrent systems**: Many independent operations
 *  - **Distributed systems**: Need location transparency
 *  - **Event-driven architecture**: Natural fit for event streams
 *  - **Fault tolerance**: Need supervision and recovery
 *
 * **Learning Path:**
 * Understand DefaultVersion first, then study:
 *  - FutureVersion (simple async)
 *  - PromiseVersion (manual async control)
 *  - Then tackle PekkoVersion (full actor model)
 */
object PekkoVersion {
  /**
   * Protocol enum defining all messages used for actor communication.
   *
   * In the Actor Model, messages define the protocol. All actors agree on message types,
   * but don't know about each other's implementation details.
   */
  enum Message {
    case Start                              // Indicates the initiation of an operation or process.
    case Greet(ref: ActorRef[Message])      // Represents a greeting message with an actor reference.
    case GreetOk(ref: ActorRef[Message])    // Acknowledges a greeting with an actor reference.
    case ReadRequest                        // Requests the retrieval of data.
    case ReadAnswer(lines: List[String])    // Contains the result of a read operation, encapsulated as a list of strings.
    case UpdateRequest                      // Requests an update operation.
    case UpdateAnswer(n: Int)               // Provides the result of an update operation, typically as an integer.
    case WriteRequest(lines: List[String])  // Sends a request to write data, provided as a list of strings.
    case WriteOk                            // Acknowledges the successful completion of a write operation.
    case GoodBye                            // Signals the conclusion of an interaction or session.
    case Shutdown                           // Requests the termination of the system or a component.
  }

  /**
   * PekkoSystem is the coordinator actor that orchestrates the read-update-write workflow.
   *
   * **Actor Responsibilities:**
   *  - Create and manage ReadWriteActor and UpdateActor
   *  - Coordinate message flow between actors
   *  - Maintain program state (protagonists list, update value n)
   *  - Trigger update logic when both responses received
   *  - Handle system lifecycle (startup, shutdown)
   *
   * @param context ActorContext provides API for actor operations (logging, spawning, etc.)
   * @param timer TimerScheduler for scheduling delayed messages (used for shutdown)
   */
  class PekkoSystem(context: ActorContext[Message], timer: TimerScheduler[Message])
    extends AbstractBehavior[Message](context){
    import Message.*

    /**
     * References to child actors, stored as Option to handle initialization timing.
     *
     * **Why Option?**
     * {{{
     * private var readWriteActor = Option.empty[ActorRef[Message]]
     * // Initially empty (None)
     * // Set when actor is created (Some(actorRef))
     * // Allows safe operations: readWriteActor.foreach(_ ! message)
     * }}}
     */
    private var readWriteActor = Option.empty[ActorRef[Message]]
    private var updateActor = Option.empty[ActorRef[Message]]

    /**
     * Program state accumulated from actor responses.
     *
     * **Coordination Pattern:**
     * {{{
     * private var protagonists = List.empty[Protagonist]  // From ReadWriteActor
     * private var n = 0                                   // From UpdateActor
     * 
     * // When both are ready, trigger update:
     * if (protagonists.nonEmpty && n != 0) {
     *   checkAndUpdateAge()
     * }
     * }}}
     */
    private var protagonists = List.empty[Protagonist]
    private var n = 0

    /**
     * This version of `updateAge` first checks if we already received information about protagonists and the number.
     * Only then this logic may work.
     *
     * @return Unit, because the result is propagated as a message `WriteRequest` to the ReadWrite actor.
     */
    private def checkAndUpdateAge(): Unit =
      if (protagonists.nonEmpty && n != 0) {
        context.log.info(s"checkAndUpdateAge")
        val updated = protagonists.map(updateAge(_, n))
        val newLines = updated.map(_.toLine)
        readWriteActor.foreach(_ ! WriteRequest(newLines))
      }

    /**
     * Updates protagonist age and logs the change.
     */
    private def updateAge(p: Protagonist, n: Int): Protagonist = {
      val newAge = p.age + n
      context.log.info(s"The age of ${p.firstName} ${p.lastName} changes from ${p.age} to $newAge")
      p.copy(age = newAge)
    }

    /**
     * Message handler — the heart of the actor.
     *
     * **Actor Message Processing:**
     * Every message sent to this actor arrives here. The actor processes one message at a time,
     * guaranteeing thread safety of its mutable state.
     *
     * **Pattern Matching with Guards:**
     * {{{
     * case GreetOk(ref) if readWriteActor.contains(ref) =>
     *   // Guard: only match if ref is the ReadWriteActor
     *   // Distinguishes between GreetOk from different actors
     * 
     * case GreetOk(ref) if updateActor.contains(ref) =>
     *   // Guard: only match if ref is the UpdateActor
     *   // Same message type, different handling
     * }}}
     *
     * **Behavior Return Values:**
     * {{{
     * Behaviors.same     // Continue with current behavior
     * Behaviors.stopped  // Terminate this actor
     * // Could also return different behavior for state machines
     * }}}
     *
     * @param msg The message to process
     * @return The behavior to adopt after processing (usually Behaviors.same)
     */
    override def onMessage(msg: Message): Behavior[Message] = msg match {
      case Start =>
        // here we create child actors and send Greet to establish communication
        context.log.info("Start")
        val rwa = ReadWriteActor(context)
        readWriteActor = Option(rwa)
        rwa ! Greet(context.self)
        val ua = UpdateActor(context)
        updateActor = Option(ua)
        ua ! Greet(context.self)
        Behaviors.same
      case GreetOk(ref) if readWriteActor.contains(ref) =>
        // we receive GreetOk from ReadWriteActor - now we know ReadWriteActor is ready and we can send ReadRequest to trigger file reading
        context.log.info("GreetOk from ReadWriteActor")
        ref ! ReadRequest
        Behaviors.same
      case GreetOk(ref) if updateActor.contains(ref) =>
        // we receive receives GreetOk from UpdateActor - now we know UpdateActor is ready and we can send UpdateRequest to trigger user input
        context.log.info("GreetOk from UpdateActor")
        ref ! UpdateRequest
        Behaviors.same
      case ReadAnswer(lines) =>
        // we receive ReadAnswer from ReadWriteActor with file contents - we parse and store protagonists, and check if we can proceed with update
        context.log.info(s"ReadAnswer from ReadWriteActor ($lines)")
        protagonists = lines.map(Protagonist.fromLine)
        checkAndUpdateAge()
        Behaviors.same
      case UpdateAnswer(n) =>
        // we receive UpdateAnswer from UpdateActor with user input - we store the update value, and check if we can proceed with update
        context.log.info(s"UpdateAnswer from UpdateActor ($n)")
        this.n = n
        checkAndUpdateAge()
        Behaviors.same
      case WriteOk =>
        // we receive WriteOk from ReadWriteActor after the file is updated - we send GoodBye to child actors, and schedule shutdown in 2 seconds
        context.log.info("WriteOk from ReadWriteActor")
        readWriteActor.foreach(_ ! GoodBye)
        updateActor.foreach(_ ! GoodBye)
        timer.startSingleTimer(Shutdown, 2.seconds)
        Behaviors.same
      case Shutdown =>
        // we receive Shutdown after delay - we terminate the actor system
        context.log.info("Shutdown!")
        context.system.terminate()
        Behaviors.stopped
      case _ =>
        // ReadRequest, WriteRequest and UpdateRequest messages are not handled - the system never receives them, only sends them to child actors
        Behaviors.same
    }
  }

  /**
   * Factory for creating the PekkoSystem actor as the root of the actor system.
   *
   * **Understanding the Layers:**
   *  - `Behaviors.withTimers`: Provides timer capability for delayed messages
   *  - `Behaviors.setup`: Provides context when actor is created
   *  - `new PekkoSystem(...)`: Creates the actual actor instance
   *  - `ActorSystem(...)`: Creates the actor system with this as root actor
   *
   * **Why This Pattern?**
   * Pekko uses behavior-based construction to ensure actors are properly initialized
   * with all necessary capabilities (context, timers, etc.) before they start processing messages.
   *
   * @return ActorSystem[Message] The actor system with PekkoSystem as root
   */
  object PekkoSystem {
    def apply(): ActorSystem[Message] = {
      val behavior = Behaviors.withTimers[Message] { timer =>
        Behaviors.setup(context => new PekkoSystem(context, timer))
      }
      ActorSystem(behavior, "pekko-system")
    }
  }

  /**
   * Actor responsible for file I/O operations (reading and writing the CSV file).
   *
   * **Separation of Concerns:**
   * In DefaultVersion, file I/O happens in main(). Here, it's delegated to a specialized actor.
   * This follows the Actor Model principle: one actor, one responsibility.
   *
   * **Actor Responsibilities:**
   *  - Read file when requested (ReadRequest → ReadAnswer)
   *  - Write file when requested (WriteRequest → WriteOk)
   *  - Maintain reference to coordinator (mainActorRef)
   *  - Terminate gracefully when asked (GoodBye)
   *
   * @param context ActorContext provides API for actor operations (logging, spawning, etc.)
   */
  class ReadWriteActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
    import Message.{Greet, ReadRequest, ReadAnswer, WriteRequest, WriteOk, GoodBye}
    import java.nio.file.{Files, Path}
    import scala.jdk.CollectionConverters.*
    import ReadWriteActor.FilePath

    private var mainActorRef: Option[ActorRef[Message]] = None

    private def read(path: Path): List[String] = Files.readAllLines(path).asScala.toList
    private def write(path: Path, lines: List[String]): Unit = Files.writeString(path, lines.mkString("\n"))

    override def onMessage(msg: Message): Behavior[Message] = msg match {
      case Greet(ref) =>
        // we receive Greet with a reference to the main actor - we save the reference, and respond with GreetOk
        context.log.info(s"ReadWriteActor: Hello, main actor! I will save a reference to you")
        mainActorRef = Some(ref)
        ref ! GreetOk(context.self)
        Behaviors.same
      case ReadRequest =>
        // we receive ReadRequest - we read the CSV file, and send the result back
        context.log.info(s"ReadWriteActor: I received a read request")
        val lines = read(FilePath)
        mainActorRef.foreach(_ ! ReadAnswer(lines))
        Behaviors.same
      case WriteRequest(lines) =>
        // we receive WriteRequest with lines to write to the file - we do it and we respond with WriteOk
        context.log.info(s"ReadWriteActor: I received a write request ($lines)")
        write(FilePath, lines)
        mainActorRef.foreach(_ ! WriteOk)
        Behaviors.same
      case GoodBye =>
        // we receive GoodBye which is a polite request to stop - we do it
        context.log.info(s"ReadWriteActor: Goodbye")
        Behaviors.stopped
      case _ =>
        // we ignore all other messages - they don't concern us here
        Behaviors.same
    }
  }

  /**
   * Companion object for ReadWriteActor: defines actor creation logic and file path.
   *
   * **Object Responsibilities:**
   *  - Define actor spawning logic (ReadWriteActor.apply())
   *  - Store constant file path
   *
   * **Why Companion Object?**
   * Provides encapsulation for the actor's initialization and configuration,
   * keeping it separate from the actor's behavior.
   */
  object ReadWriteActor {
    private val FilePath: Path = Paths.get("resources/protagonists.csv")

    def apply(context: ActorContext[?]): ActorRef[Message] = {
      val behavior = Behaviors.setup(new ReadWriteActor(_))
      context.spawn(behavior, "read-write-actor")
    }
  }

  /**
   * Actor responsible for asking the user for an update value.
   *
   * **Actor Responsibilities:**
   *  - Ask user for integer input (UpdateRequest → UpdateAnswer)
   *  - Maintain reference to coordinator (mainActorRef)
   *  - Terminate gracefully when asked (GoodBye)
   *
   * **Why Separate Actor for User Input?**
   * User input blocks! We don't want to block the coordinator.
   * By delegating to UpdateActor, we keep the coordinator responsive.
   *
   * @param context ActorContext provides API for actor operations (logging, spawning, etc.)
   */
  class UpdateActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
    import Message.{Greet, UpdateRequest, UpdateAnswer, GoodBye}
    private var mainActorRef: Option[ActorRef[Message]] = None

    private def askForUpdate(): Int = {
      printf("By how much should I update the age? ")
      val answer = scala.io.StdIn.readLine()
      answer.toInt
    }

    override def onMessage(msg: Message): Behavior[Message] = msg match {
      case Greet(ref) =>
        // we receive Greet with a reference to the main actor - we save the reference, and respond with GreetOk
        context.log.info(s"UpdateActor: Hello, main actor! I will save a reference to you")
        mainActorRef = Some(ref)
        ref ! GreetOk(context.self)
        Behaviors.same
      case UpdateRequest =>
        // we receive UpdateRequest from the main actor - we ask the user for input and when received, we send it back
        context.log.info(s"UpdateActor: I received an update request")
        val n = askForUpdate()
        mainActorRef.foreach(_ ! UpdateAnswer(n))
        Behaviors.same
      case GoodBye =>
        // we receive GoodBye which is a polite request to stop - we do it
        context.log.info(s"ReadWriteActor: Goodbye")
        Behaviors.stopped
      case _ =>
        // we ignore all other messages - they don't concern us here
        Behaviors.same
    }
  }

  /**
   * Companion object for UpdateActor: defines actor creation logic.
   */
  object UpdateActor {
    def apply(context: ActorContext[?]): ActorRef[Message] = {
      val behavior = Behaviors.setup(new UpdateActor(_))
      context.spawn(behavior, "update-actor")
    }
  }

  /**
   * Main entry point for the PekkoVersion application.
   *
   * **Responsibilities:**
   *  - Create the ActorSystem (PekkoVersion.PekkoSystem())
   *  - Send the initial Start message
   *  - Await termination to keep the application alive
   *  - Print start and complete messages
   *
   * **Why Await Termination?**
   * Without Await.result(), main() would exit immediately after sending Start,
   * and the ActorSystem would terminate before doing anything.
   * Await.result() blocks the main thread until the ActorSystem terminates itself
   * (after a delay, triggered by WriteOk).
   */
  /* @main */ def main(): Unit = {
    import PekkoVersion.Message.Start

    println("Pekko Version Start")
    val system = PekkoSystem()
    system ! Start

    try {
      // Block the main thread until the system terminates
      Await.result(system.whenTerminated, Duration.Inf)
    } finally {
      println("Pekko Version Complete")
    }
  }
}
