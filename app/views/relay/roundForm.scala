package views.html.relay

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.relay.RelayRound.Sync.UpstreamUrl.LccRegex
import lila.relay.RelayRoundForm.Data
import lila.relay.{ RelayRound, RelayTour }

object roundForm:

  import trans.broadcast.*

  def create(form: Form[Data], tour: RelayTour)(using PageContext) =
    layout(newBroadcast.txt())(
      boxTop(h1(a(href := routes.RelayTour.edit(tour.id))(tour.name), " • ", addRound())),
      standardFlash,
      inner(form, routes.RelayRound.create(tour.id), tour, create = true)
    )

  def edit(rt: RelayRound.WithTour, form: Form[Data])(using PageContext) =
    layout(rt.fullName)(
      boxTop(
        h1(dataIcon := licon.Pencil, cls := "text")(
          a(href := routes.RelayTour.edit(rt.tour.id))(rt.tour.name),
          " • ",
          a(href := rt.path)(rt.round.name)
        )
      ),
      inner(form, routes.RelayRound.update(rt.round.id), rt.tour, create = false),
      div(cls := "relay-form__actions")(
        postForm(action := routes.RelayRound.reset(rt.round.id))(
          submitButton(
            cls := "button button-red button-empty confirm"
          )(
            strong(resetRound()),
            em(deleteAllGamesOfThisRound())
          )
        ),
        postForm(action := routes.Study.delete(rt.round.id))(
          submitButton(
            cls := "button button-red button-empty confirm"
          )(strong(deleteRound()), em(definitivelyDeleteRound()))
        )
      )
    )

  private def layout(title: String)(body: Modifier*)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relay.form"),
      moreJs = jsModule("flatpickr")
    ):
      main(cls := "page-small box box-pad")(body)

  private def inner(form: Form[Data], url: play.api.mvc.Call, t: RelayTour, create: Boolean)(using
      ctx: PageContext
  ) =
    val isLcc = form("syncUrl").value.exists(LccRegex.matches)
    postForm(cls := "form3", action := url)(
      div(cls := "form-group")(
        bits.howToUse,
        (create && t.createdAt.isBefore(nowInstant.minusMinutes(1))).option:
          p(dataIcon := licon.InfoCircle, cls := "text"):
            theNewRoundHelp()
      ),
      form3.globalError(form),
      form3.split(
        form3.group(form("name"), roundName(), half = true)(form3.input(_)(autofocus)),
        isGranted(_.StudyAdmin).option(
          form3.group(
            form("caption"),
            "Homepage spotlight custom round name",
            help = raw("Leave empty to use the round name").some,
            half = true
          ):
            form3.input(_)
        )
      ),
      form3.group(
        form("syncUrl"),
        sourceUrlOrGameIds(),
        help = frag(
          sourceUrlHelp(),
          br,
          gameIdsHelp(),
          br,
          "Or leave empty to push games from another program."
        ).some
      )(form3.input(_)),
      form3
        .group(form("syncUrlRound"), roundNumber(), help = frag("Only for livechesscloud source URLs").some)(
          form3.input(_, typ = "number")
        )(cls := (!isLcc).option("none")),
      form3.split(
        form3.group(
          form("startsAt"),
          startDate(),
          help = startDateHelp().some,
          half = true
        )(form3.flatpickr(_, minDate = None)),
        form3.checkbox(
          form("finished"),
          completed(),
          help = completedHelp().some,
          half = true
        )
      ),
      form3.split(
        form3.group(
          form("delay"),
          raw("Delay in seconds"),
          help = frag(
            "Optional, how long to delay moves coming from the source.",
            br,
            "Add this delay to the start date of the event. E.g. if a tournament starts at 20:00 with a delay of 15 minutes, set the start date to 20:15."
          ).some,
          half = true
        )(form3.input(_, typ = "number")),
        isGranted(_.StudyAdmin).option(
          form3.group(
            form("period"),
            periodInSeconds(),
            help = periodInSecondsHelp().some,
            half = true
          )(form3.input(_, typ = "number"))
        )
      ),
      form3.actions(
        a(href := routes.RelayTour.show(t.slug, t.id))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
