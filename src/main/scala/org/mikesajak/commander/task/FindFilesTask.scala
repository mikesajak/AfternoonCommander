package org.mikesajak.commander.task

import javafx.{concurrent => jfxc}
import org.apache.commons.io.{FilenameUtils, IOCase}
import org.mikesajak.commander.FileTypeManager
import org.mikesajak.commander.fs.{VDirectory, VFile, VPath}
import org.mikesajak.commander.ui.ResourceManager
import org.mikesajak.commander.util.Utils._
import org.mikesajak.commander.util.{FileUtils, ReadLineIterator}
import scribe.Logging

import java.io.{BufferedReader, InputStreamReader}
import java.util.regex.Pattern
import scala.util.Using

case class SearchCriteria(searchString: String, caseSensitive: Boolean, regex: Boolean, inverse: Boolean)
case class Search(filenameCriteria: SearchCriteria, contentCriteria: Option[SearchCriteria])

case class SearchProgress(curPath: VPath, dirStats: DirStats, results: Seq[VPath])

class FindFilesTask(searchDef: Search, startDirectory: VDirectory, resourceMgr: ResourceManager,
                    fileTypeMgr: FileTypeManager)
    extends jfxc.Task[SearchProgress] with Logging {
  private type MatchFunc = String => Boolean

  override def call(): SearchProgress = try {
    updateProgress(-1, -1)
    val result = search(startDirectory, mkTextMatchFunc(searchDef.filenameCriteria),
                        searchDef.contentCriteria.map(mkContentMatchFunc),
                        SearchStatus(Seq.empty, DirStats.Empty))(new IndentScope)
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
    def add(partResults: Seq[VPath], partStats: DirStats): SearchStatus =
      SearchStatus(results ++ partResults, stats + partStats)
  }

  private def search(dir: VDirectory, filenameMatchFunc: MatchFunc, contentMatchFunc: Option[MatchFunc],
                     results: SearchStatus)(indent: Scope): SearchStatus = {
    try {
      checkCancelled(SearchProgress(dir, results.stats, results.results))

      val matchingFilenames = dir.childFiles.filter(f => filenameMatchFunc.apply(f.name))
      val matchingFiles = matchingFilenames.filter(vfile => contentMatchFunc.forall(func => matchFileContent(vfile, func)))

      val filesResult = results.add(matchingFiles, DirStats.ofFiles(dir.childFiles))
      updateValue(SearchProgress(dir, filesResult.stats, filesResult.results))

      val stageResult =
        if (contentMatchFunc.isEmpty) {
          val matchingDirs = dir.childDirs.filter(d => filenameMatchFunc.apply(d.name))
          val filesAndDirsResult = filesResult.add(matchingDirs, DirStats.ofDirs(dir.childDirs))
          updateValue(SearchProgress(dir, filesAndDirsResult.stats, filesAndDirsResult.results))
          filesAndDirsResult
        } else filesResult

      dir.childDirs.foldLeft(stageResult)((res, childDir) => search(childDir, filenameMatchFunc, contentMatchFunc, res)(indent.up()))
    } finally {
      indent.down()
    }
  }

  private def matchFileContent(vfile: VFile, contentMatchFunc: MatchFunc): Boolean = {
    val mimeType = fileTypeMgr.mimeTypeOf(vfile)
    if (mimeType.startsWith("text")) {
        try {
          getMatchedLines(vfile, contentMatchFunc).nonEmpty
        } catch {
          case e: Exception =>
            logger.info(s"Error reading file: ${vfile.absolutePath}. Skipping file content matching.", e)
            false
        }
    } else {
      logger.info(s"Skipping content matching for non-text/binary file: ${vfile.name}")
      false
    }
  }

  private def getMatchedLines(vfile: VFile, contentMatchFunc: MatchFunc) = {
    val charset = FileUtils.detectCharset(vfile)
    Using.resource(new BufferedReader(new InputStreamReader(vfile.inStream, charset))) { reader =>
      new ReadLineIterator(reader)
        .zipWithIndex
        .filter { case (line, idx) => contentMatchFunc.apply(line) }
        .map { case (line, idx) => (idx, line) }
        .toSeq
    }
  }

  private def mkTextMatchFunc(criteria: SearchCriteria): MatchFunc = {
    if (criteria.searchString.isBlank) _ => true
    else {
      val plainFunc = if (criteria.regex) {
        input: String =>
          val pattern = mkRegexPattern(criteria.searchString, criteria.caseSensitive)
          pattern.matcher(input).find()
      } else {
        input: String =>
          FilenameUtils.wildcardMatch(input, criteria.searchString,
                                      if (criteria.caseSensitive) IOCase.SENSITIVE else IOCase.INSENSITIVE)
      }

      if (criteria.inverse) input => !plainFunc(input)
      else plainFunc
    }
  }

  private def mkContentMatchFunc(criteria: SearchCriteria): MatchFunc = {
    val searchString = if (criteria.regex) criteria.searchString
                       else Pattern.quote(criteria.searchString)
    val pattern = mkRegexPattern(searchString, criteria.caseSensitive)

    val plainFunc = { input: String => pattern.matcher(input).find() }

    if (criteria.inverse) input => !plainFunc(input)
    else plainFunc
  }


  private def mkRegexPattern(searchString: String, caseSensitive: Boolean) = {
    if (caseSensitive) Pattern.compile(searchString)
    else Pattern.compile(searchString, Pattern.CASE_INSENSITIVE)
  }

  private def checkCancelled(status: => SearchProgress): Unit = {
    if (isCancelled) {
      logger.debug(s"Cancel request was detected - stopping current task.")
      throw CancelledException(status)
    }
  }

}
