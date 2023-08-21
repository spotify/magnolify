import laika.ast.LengthUnit.px
import laika.ast.Path.Root
import laika.ast.{Image, Styles, TemplateString}
import laika.bundle.ExtensionBundle
import laika.helium.Helium
import laika.helium.config.*
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.theme.ThemeProvider
import laika.theme.config.Color
import sbt.Def._
import sbt.Keys.{organizationName, scmInfo, licenses}

object SiteSettings {

  // colors
  private val spotifyGreen = Color.hex("1ED760")
  private val black = Color.hex("000000")
  private val white = Color.hex("FFFFFF")
  private val red = Color.hex("FF0000")

  private val text = Color.hex("535353")

  private val background = white
  private val backgroundMedium = white
  private val backgroundLight = white

  private val primary = Color.hex("202F72")
  private val primaryMedium = Color.hex("D6E3FF")
  private val primaryLight = white

  private val secondary = spotifyGreen
  private val secondaryMedium = spotifyGreen
  private val secondaryLight = Color.hex("E0FBE9")

  private val info = Color.hex("C8E0FC")
  private val infoLight = Color.hex("ECF4FE")

  private val warning = Color.hex("FFD97E")
  private val warningLight = Color.hex("FFF0CC")

  private val error = Color.hex("FFD2D7")
  private val errorLight = Color.hex("FFEBED")

  // resources and links
  private val siteFavIcon = Favicon.internal(Root / "images" / "favicon.ico", "16x16")

  private val siteHomeLink = ImageLink.internal(
    Root / "index.md",
    Image.internal(Root / "images" / "logo.png")
  )

  private val apiLink = IconLink.internal(
    Root / "api" / "index.html",
    HeliumIcon.api,
    options = Styles("svg-link")
  )

  private val githubLink = setting {
    IconLink.external(
      scmInfo.toString,
      HeliumIcon.github,
      options = Styles("svg-link")
    )
  }

  // template
  private val footer = setting {
    val year = java.time.LocalDate.now().getYear
    val licensePhrase = licenses.value.headOption.fold("") {
      case (name, url) =>
        s""" distributed under the <a href="${url.toString}">$name</a> license"""
    }
    TemplateString(s"""Copyright $year ${organizationName.value}""" + licensePhrase)
  }

  val siteTheme: Initialize[ThemeProvider] = setting {
    Helium
      .defaults
      .site
      .layout(topBarHeight = px(50))
      .site
      .darkMode
      .disabled
      .site
      .favIcons(siteFavIcon)
      .site
      .footer(footer.value)
      .site
      .topNavigationBar(
        homeLink = siteHomeLink,
        navLinks = List(apiLink, githubLink.value)
      )
      .site
      .themeColors(
        primary = primary,
        secondary = secondary,
        primaryMedium = primaryMedium,
        primaryLight = primaryLight,
        text = text,
        background = background,
        bgGradient = (backgroundMedium, backgroundLight)
      )
      .site
      .messageColors(
        info = info,
        infoLight = infoLight,
        warning = warning,
        warningLight = warningLight,
        error = error,
        errorLight = errorLight
      )
      .build
  }

  val siteExtensions: Seq[ExtensionBundle] = Seq(GitHubFlavor, SyntaxHighlighting)
}
