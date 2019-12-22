package org.mikesajak.commander.ui.keys

import org.mikesajak.commander.ui.Action
import org.mikesajak.commander.ui.keys.KeyActionMapper.{KeyActionMapping, KeyInput}
import org.mikesajak.commander.ui.keys.Modifier.{Alt, Ctrl, Meta, Shift}
import scalafx.scene.input.{KeyCode, KeyEvent}

object KeyActionMapper {
  import Action._

  case class KeyInput(code: KeyCode, modifiers: Set[Modifier])
  object KeyInput {
    def apply(code: KeyCode): KeyInput = new KeyInput(code, Set.empty)
    def apply(code: KeyCode, modifiers0: Set[Modifier]) = new KeyInput(code, modifiers0)
    def apply(code: KeyCode, modifiers0: Modifier*) = new KeyInput(code, modifiers0.toSet)

    def apply(keyEvent: KeyEvent): KeyInput = {
      val modifiers: Set[Modifier] = Set(if (keyEvent.altDown) Some(Alt) else None,
                                         if (keyEvent.controlDown) Some(Ctrl) else None,
                                         if (keyEvent.shiftDown) Some(Shift) else None,
                                         if (keyEvent.metaDown) Some(Meta) else None)
          .flatten
      new KeyInput(keyEvent.code, modifiers)
    }
  }

  type KeyActionMapping = Map[KeyInput, Action]

  val Default: KeyActionMapping = Map(
    KeyInput(KeyCode.F3) -> View,
    KeyInput(KeyCode.F4) -> Edit,
    KeyInput(KeyCode.F5) -> Copy,
    KeyInput(KeyCode.F6) -> Move,
    KeyInput(KeyCode.F7) -> MkDir,
    KeyInput(KeyCode.F8) -> Delete,
    KeyInput(KeyCode.F10) -> Exit,

    KeyInput(KeyCode.F2) -> CountDirStats,
    KeyInput(KeyCode.F11) -> ShowProperties,

    KeyInput(KeyCode.F7, Alt) -> FindFiles,

    KeyInput(KeyCode.R, Ctrl) -> Refresh)
}

class KeyActionMapper(val mapping: KeyActionMapping) {
  def actionForKey(keyAction: KeyInput): Option[Action] = mapping.get(keyAction)
}




