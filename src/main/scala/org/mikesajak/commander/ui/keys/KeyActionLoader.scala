package org.mikesajak.commander.ui.keys

import java.io.{BufferedWriter, FileWriter}

import org.mikesajak.commander.ui.Action
import org.mikesajak.commander.ui.keys.KeyActionMapper.{KeyActionMapping, KeyInput}
import org.mikesajak.commander.util.Utils.using
import scalafx.scene.input.KeyCode

import scala.io.Source

class KeyActionLoader {
  def save(keyActionMapping: KeyActionMapping, filename: String): Unit = {
    using(new BufferedWriter(new FileWriter(filename))) { writer =>
      for ((keyInput, action) <- keyActionMapping) {
        if (keyInput.modifiers.nonEmpty) {
          val modifiersStr = keyInput.modifiers.map(_.toString).reduce((acc, mod) => s"$acc+$mod")
          writer.write(s"$modifiersStr+")
        }
        writer.write(s"${keyInput.code}: $action")
        writer.newLine()
      }
    }
  }

  private val KeyInputPattern = raw"((.+\+)?(.+))\s*:\s*(\S+)".r

  def load(filename: String): KeyActionMapping = load(Source.fromFile(filename))

  def load(source: Source): KeyActionMapping = {
    using(source) { src =>
      src.getLines().toList
            .flatMap(parseKeyInput)
            .toMap
    }
  }

  def parseKeyInput(line: String): Option[(KeyInput, Action)] = line match {
    case KeyInputPattern(_, null, keyStr, actionStr) =>
      Some(KeyInput(KeyCode.keyCode(keyStr)), Action.withName(actionStr))

    case KeyInputPattern(_, modifiersStr, keyStr, actionStr) =>
      val modifiers = modifiersStr.split(raw"\+")
                                  .map(Modifier.withName)
                                  .toSet
      Some(KeyInput(KeyCode.keyCode(keyStr), modifiers), Action.withName(actionStr))

    case _ => None
  }
}

object KeyActionLoader {
  def main(args: Array[String]): Unit = {
    val loader = new KeyActionLoader
    //    loader.save(KeyActionMapper.Default, "actions.keymap")
    val keyMap = loader.load("actions.keymap")

    println(keyMap)

    //    test("F2: CountDirStats")
    //    test("Ctrl+F2: CountDirStats")
    //    test("Ctrl+Alt+F2: CountDirStats")
    //    test("Shift+Ctrl+Alt+F2: CountDirStats")

  }

  private def test(line: String): Unit = {
    val r1 = (new KeyActionLoader).parseKeyInput(line)
    println(r1)
  }

}
