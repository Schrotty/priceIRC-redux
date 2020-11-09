package de.rubenmaurer.price.core.antlr

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import de.rubenmaurer.price.antlr.{IRCLexer, IRCParser}
import de.rubenmaurer.price.core.{Parse, Request}
import org.antlr.v4.runtime._

object Parser {
  def apply(): Behavior[Request[String]] = {
    Behaviors.setup { _ =>
      Behaviors.receive[Request[String]] { (context, request) =>
        request.command match {
          case Parse(payload) =>
            parse(payload)
            Behaviors.same

          case _ => Behaviors.same
        }
      }
    }
  }

  def parse(message: String): Unit = {
    println("\nEvaluating expression" + message)

    val lexer = new IRCLexer(new ANTLRInputStream(message))
    val tokens = new CommonTokenStream(lexer)
    val parser = new IRCParser(tokens)

    parser.addErrorListener(new PricefieldErrorListener())
    parser.response().enterRule(new PricefieldListener())
  }
}
