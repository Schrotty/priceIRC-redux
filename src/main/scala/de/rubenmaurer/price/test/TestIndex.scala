package de.rubenmaurer.price.test

object TestIndex {
  private val _suites: Map[String, String] = Map(
    "unit" -> "UnitTest"
  )

  private val _assignments: Map[String, List[String]] = Map(
    "ALL" -> List(_suites.apply("unit"))
  )

  def getAll(keys: String*): List[String] =
    getSuites(keys).appendedAll(getAssignments(keys))

  def getSuites(suits: Seq[String]): List[String] =
    _suites.filter(suite => suits.contains(suite._1)).values.toList

  def getAssignments(assignments: Seq[String]): List[String] =
    _assignments.filter(as => assignments.contains(as._1)).values.flatten.toList
}
