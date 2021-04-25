package org.mikesajak.commander.task

import javafx.{concurrent => jfxc}
import org.apache.commons.io.{FilenameUtils, IOCase}
import org.mikesajak.commander.fs.{VDirectory, VPath}
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.Utils._
import scribe.Logging

import java.util.regex.Pattern

case class SearchCriteria(searchString: String, caseSensitive: Boolean, regex: Boolean, inverse: Boolean)
case class Search(filenameCriteria: SearchCriteria, contentCriteria: Option[SearchCriteria])

case class SearchProgress(curPath: VPath, dirStats: DirStats, results: Seq[VPath])

class FindFilesTask(searchDef: Search, startDirectory: VDirectory, resourceMgr: ResourceManager)
    extends jfxc.Task[SearchProgress] with Logging {
  private type MatchFunc = String => Boolean

  override def call() : SearchProgress = try {
    updateProgress(-1, -1)
    val result = search(startDirectory, mkMatchFunc(), SearchStatus(Seq.empty, DirStats.Empty))(new IndentScope)
    updateProgress(1, 1)
    SearchProgress(startDirectory, result.stats, result.results)
  } catch {
    case ce: CancelledException[SearchProgress] =>
      logger.info(s"Task $this has been cancelled.")
      updateMessage(resourceMgr.getMessage("task.cancelled"))
      ce.value
    case e: Exception =>
      logger.info(s"Exception", e)
      throw e
  }

  case class SearchStatus(results: Seq[VPath], stats: DirStats) {
    def add(partResults: Seq[VPath], partStats: DirStats) =
      SearchStatus(results ++ partResults, stats + partStats)
  }

  private def search(dir: VDirectory, matchFunc: MatchFunc, results: SearchStatus)(indent: Scope): SearchStatus =
    try {
      checkCancelled(SearchProgress(dir, results.stats, results.results))

      val matchingFiles = dir.childFiles.filter(f => matchFunc.apply(f.name))

      val result1 = results.add(matchingFiles, DirStats.ofFiles(dir.childFiles))
      updateValue(SearchProgress(dir, result1.stats, result1.results))

      val matchingDirs = dir.childDirs.filter(d => matchFunc.apply(d.name))
      val result2 = result1.add(matchingDirs, DirStats.ofDirs(dir.childDirs))
      updateValue(SearchProgress(dir, result2.stats, result2.results))

      dir.childDirs.foldLeft(result2)((res, childDir) => search(childDir, matchFunc, res)(indent.up()))
    } finally {
      indent.down()
    }

  private def mkMatchFunc() = {
    mkTextMatchFunc(searchDef.filenameCriteria)
    // todo: content criteria
  }

  private def mkTextMatchFunc(criteria: SearchCriteria): MatchFunc = {
    val plainFunc = if (criteria.regex) {
      val pattern = if (criteria.caseSensitive) Pattern.compile(criteria.searchString)
                    else Pattern.compile(criteria.searchString, Pattern.CASE_INSENSITIVE)
      input: String => pattern.matcher(input).matches()
    } else {
      input: String =>
        FilenameUtils.wildcardMatch(input, criteria.searchString,
                                    if (criteria.caseSensitive) IOCase.SENSITIVE else IOCase.INSENSITIVE)
    }

    if (criteria.inverse) input => !plainFunc(input)
    else plainFunc
  }

  private def checkCancelled(status: => SearchProgress): Unit = {
    if (isCancelled) {
      logger.debug(s"Cancel request was detected - stopping current task.")
      throw CancelledException(status)
    }
  }

}
