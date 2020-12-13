package de.rubenmaurer.price.core.parser.antlr

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.{BaseErrorListener, RecognitionException, Recognizer}

class PricefieldErrorListener extends BaseErrorListener {
  private var _exceptions: List[String] = List[String]()

  def exceptions: List[String] = _exceptions

  /**
   * Handles a ParseException.
   *
   * @param recognizer         the recognizer
   * @param offendingSymbol    the offending symbol
   * @param line               the line number
   * @param charPositionInLine the character position
   * @param msg                the message
   * @param e                  the exception
   * @throws ParseCancellationException the exception
   */
  @throws[ParseCancellationException]
  override def syntaxError(recognizer: Recognizer[_, _], offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException): Unit = {
    _exceptions = String.format("%s%s!", msg.substring(0, 1).toUpperCase, msg.substring(1)) :: _exceptions
    //throw e
  }
}
