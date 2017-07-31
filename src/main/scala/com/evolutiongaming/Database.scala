package com.evolutiongaming

import com.evolutiongaming.Models.Table

class Database(private var values: Vector[Table] = Vector.empty, private var latestId: Int = 0) {
  def insert(table: Models.NewTable, afterId: Int): Option[Table] = {
    val newTable = Table(nextId, table.name, table.participants)
    if (afterId == -1) {
      values = newTable +: values
      Some(newTable)
    } else {
      val idx = values.indexWhere(_.id == afterId)
      if (idx == -1) {
        None
      } else {
        val (left, right) = values.splitAt(idx + 1)
        values = (left :+ newTable) ++ right
        Some(newTable)
      }
    }
  }

  def update(table: Models.Table): Option[Table] = {
    val idx = values.indexWhere(_.id == table.id)
    if (idx == -1) {
      None
    } else {
      values = values.updated(idx, table)
      Some(table)
    }
  }

  def remove(id: Int): Option[Unit] = {
    val idx = values.indexWhere(_.id == id)
    if (idx == -1) None
    else {
      values = values.filter(_.id != id)
      Some(())
    }
  }

  private def nextId = { latestId += 1; latestId }

  def tables = values
}
