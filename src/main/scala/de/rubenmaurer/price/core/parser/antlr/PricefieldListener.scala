package de.rubenmaurer.price.core.parser.antlr

import de.rubenmaurer.price.antlr.{IRCBaseListener, IRCParser}

class PricefieldListener extends IRCBaseListener {
  override def enterResponse(ctx: IRCParser.ResponseContext): Unit = {

  }
}
