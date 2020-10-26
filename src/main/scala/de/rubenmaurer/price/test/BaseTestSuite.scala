package de.rubenmaurer.price.test

import de.rubenmaurer.price.core.facade._
import org.scalactic.Requirements.requireNonNull
import org.scalatest.events.{SeeStackDepthException, TestFailed}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest._

import scala.collection.mutable.ListBuffer

class BaseTestSuite(session: Session, testName: String) extends AnyFunSuite with BeforeAndAfter {
  before {
    session.start(testName)
  }

  after {
    session.stop()
  }

  override def runTests(testName: Option[String], args: Args): Status = {
    requireNonNull(testName, args)

    import args._

    val theTestNames = testNames
    val statusBuffer = new ListBuffer[Status]()

    // If a testName is passed to run, just run that, else run the tests returned
    // by testNames.
    testName match {
      case Some(tn) =>
        val (filterTest, _) = filter(tn, tags, suiteId)
        if (!filterTest) {
          var status: Status = new StatefulStatus

          try {
            status = runTest(tn, args)
          } catch {
            case e: Throwable =>
              status = FailedStatus
              reporter(TestFailed(tracker.nextOrdinal(), e.getMessage, this.suiteName, this.suiteId, Some(this.getClass.getName), testName.get, "testText", null, null, Some(e), Some(50), Some(null), Option(SeeStackDepthException), this.rerunner, Option(null)))

          } finally {
            statusBuffer += status
          }
        }

      case None =>
        for ((tn, _) <- filter(theTestNames, tags, suiteId)) {
          if (!stopper.stopRequested) {
            statusBuffer += runTest(tn, args)
          }
        }
    }

    new CompositeStatus(Set.empty ++ statusBuffer)
  }
}
