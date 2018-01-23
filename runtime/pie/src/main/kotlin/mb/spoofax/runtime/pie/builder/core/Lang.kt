package mb.spoofax.runtime.pie.builder.core

import mb.pie.runtime.core.*
import mb.pie.runtime.core.stamp.PathStampers
import mb.vfs.path.PPath
import org.metaborg.core.build.CommonPaths
import org.metaborg.core.language.*

typealias TransientLangImpl = OutTransientEquatable<ILanguageImpl, LanguageIdentifier>

class CoreLoadLang : Func<PPath, TransientLangImpl> {
  companion object {
    val id = "coreLoadLang"
  }

  override val id = Companion.id
  override fun ExecContext.exec(input: PPath): TransientLangImpl {
    val spoofax = Spx.spoofax()
    val resource = input.fileObject
    val request: IComponentCreationConfigRequest
    if(resource.isFile) {
      request = spoofax.languageComponentFactory.requestFromArchive(resource)
      require(input, PathStampers.hash)
    } else {
      request = spoofax.languageComponentFactory.requestFromDirectory(resource)
      val paths = CommonPaths(resource)
      require(paths.targetMetaborgDir().pPath, PathStampers.hash)
    }
    val config = spoofax.languageComponentFactory.createConfig(request)
    val component = spoofax.languageService.add(config)
    val impl = component.contributesTo().first()
    return OutTransientEquatableImpl(impl, impl.id())
  }
}

fun ExecContext.loadLangRaw(input: PPath) = requireOutput(CoreLoadLang::class, CoreLoadLang.Companion.id, input)
fun ExecContext.loadLang(input: PPath) = loadLangRaw(input).v