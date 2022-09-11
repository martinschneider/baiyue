#!/usr/bin/python
# -*- coding: utf-8 -*-

import yaml
from yaml.loader import SafeLoader
from airium import Airium

with open("xiaobaiyue.yml") as f:
    data = yaml.load(f, Loader=SafeLoader)

    markers = ""
    for x in data:
        if x["location"]:
            markers += """
                markers["{}"] = L.marker([{}], {{icon: $("#{}").prop("checked") ? GREEN_ICON : RED_ICON}}).bindPopup('<a href="#{}">{} {}</a><br/><button class="btn" data-clipboard-text="{}">Copy coordinates</button>').addTo(map);""".format(
                x["OSM"],
                x["location"],
                x["OSM"],
                x["OSM"],
                x["chinese"],
                x["english"],
                x["location"]
            )
a = Airium()
a("<!DOCTYPE html>")
with a.html(lang="en"):
    with a.head():
        a.meta(charset="utf-8")
        a.meta(content="width=device-width, initial-scale=1", name="viewport")
        a.title(_t="Taiwan's Xiaobaiyue 小百岳")
        a.link(
            href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.min.css",
            rel="stylesheet",
        )
        a.link(
            href="https://cdn.datatables.net/responsive/2.3.0/css/responsive.dataTables.min.css",
            rel="stylesheet",
        )
        a.link(
            href="https://code.jquery.com/ui/1.13.1/themes/base/jquery-ui.css",
            rel="stylesheet",
        )
        a.link(
            href="https://unpkg.com/leaflet@1.8.0/dist/leaflet.css", rel="stylesheet"
        )
        a.link(
            href="https://cdnjs.cloudflare.com/ajax/libs/vegas/2.5.4/vegas.css",
            rel="stylesheet",
        )
        a.link(
            href="https://cdnjs.cloudflare.com/ajax/libs/Leaflet.awesome-markers/2.0.2/leaflet.awesome-markers.css",
            rel="stylesheet",
        )
        a.link(href="style.css", rel="stylesheet")
        a.link(href="favicon.png", rel="icon")
        a.meta(content="chrome=1", **{"http-equiv": "X-UA-Compatible"})
        a.meta(content="True", name="HandheldFriendly")
        a.meta(content="320", name="MobileOptimized")
        a.meta(content="width=device-width, initial-scale=1.0", name="viewport")
        a.meta(content="no-referrer", name="referrer")
        a.meta(content="Martin Schneider", name="author")
        a.meta(content='Taiwan"s Xiaobaiyue 小百岳', name="description")
        a.base(target="_parent")
        a.script(src="https://code.jquery.com/jquery-3.6.0.min.js")
        a.script(src="https://code.jquery.com/ui/1.13.1/jquery-ui.js")
        a.script(src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.min.js")
        a.script(
            src="https://cdn.datatables.net/responsive/2.3.0/js/dataTables.responsive.min.js"
        )
        a.script(src="https://cdn.datatables.net/plug-ins/1.12.1/sorting/natural.js")
        a.script(src="https://unpkg.com/leaflet@1.8.0/dist/leaflet.js")
        a.script(src="https://cdnjs.cloudflare.com/ajax/libs/vegas/2.5.4/vegas.min.js")
        a.script(
            src="https://cdnjs.cloudflare.com/ajax/libs/Leaflet.awesome-markers/2.0.2/leaflet.awesome-markers.min.js"
        )
        a.script(src="https://kit.fontawesome.com/288fa10781.js")
        a.script(
            src="https://cdnjs.cloudflare.com/ajax/libs/clipboard.js/2.0.10/clipboard.min.js"
        )
        a.script(
            async_="",
            src="https://gc.zgo.at/count.js",
            **{"data-goatcounter": "https://xiaobaiyue.goatcounter.com/count"}
        )
        a.script(src="script.js")
        with a.script():
            a(
                """
          // Number of peaks
          const TOTAL = 116;
          $(document).ready(function () {{{}}});
          """.format(
                    markers
                )
            )
    with a.body():
        with a.div(klass="container"):
            with a.div(id="description"):
                with a.h1():
                    a("🌄 Taiwan's 小百岳 Xiaobaiyue 🌄")
                with a.p():
                    a(
                        """In an effort to promote national mountaineering, the Sports Committee of Taiwan identified 100 entry-level hikes. These peaks are known as the 小百岳 Xiaobaiyue, Taiwan's 100 small peaks."""
                    )
                with a.p():
                    a(
                        """This page aims to collect information about all current and former 小百岳 and let's you keep track of which ones you've already been to."""
                    )
                with a.p(klass="small"):
                    a("Sources can be found on ")
                    a.a(
                        href="https://zh.m.wikipedia.org/zh-tw/%E5%8F%B0%E7%81%A3%E5%B0%8F%E7%99%BE%E5%B2%B3%E5%88%97%E8%A1%A8",
                        _t="Wikipedia.",
                    )
                    a("Names, location and elevation data have been updated from ")
                    a.a(href="https://www.openstreetmap.org", _t="OpenStreetMap.")
                    a("Please ")
                    a.a(
                        href="https://docs.google.com/spreadsheets/d/1F9N4VxXvDMfUJgLy6YUt6U7PTh9kFNGJjEzrriyXS_Y/edit?usp=sharing",
                        _t="report errors",
                    )
                    a("or provide ")
                    a.a(href="mailto:xiaobaiyue@5164.at", _t="other feedback.")
                with a.p(klass="small"):
                    a(
                        "This page is ad-free. If you find it useful, consider "
                    )
                    a.a(href="http://buymeacoffee.com/mschneider", _t="donating.")
                    a("❤️ Thank you!")
            with a.div(id="map"):
                with a.div(klass="leaflet-top leaflet-right"):
                    with a.select(klass="presets", id="layer-selector"):
                        a.option(value="0", _t="台灣魯地圖")
                        a.option(value="1", _t="OpenStreetMap", selected=True)
                    a.input(klass="presets", type="button", value="🇹🇼 台灣 Taiwan", onclick="map.setView(DEFAULT_COORDINATES, DEFAULT_ZOOM)")
        with a.div(id="progress-container"):
            a.div(id="progress", _t="25%")
        with a.table(klass="display", id="xiaobaiyue", width="100%"):
            with a.thead():
                with a.tr():
                    a.th(_t="✔️", **{"data-priority": "1"})
                    a.th(_t="2017", **{"data-priority": "5"})
                    a.th(_t="2006")
                    a.th(_t="2003")
                    a.th(_t="Chinese name", **{"data-priority": "1"})
                    a.th(_t="English name", **{"data-priority": "2"})
                    a.th(_t="Height", **{"data-priority": "3"})
                    a.th(_t="Location", **{"data-priority": "4"})
            with a.tbody():
                for x in data:
                    with a.tr():
                        with a.td(klass="center"):
                            a.a(name="{}".format(str(x["OSM"])))
                            a.input(
                                type="checkbox",
                                id="{}".format(x["OSM"]),
                                onClick="""
                            var checkbox = $("#{}");
                            var checked = $("#{}").prop("checked");
                            markers[{}].setIcon(checked ? GREEN_ICON : RED_ICON);
                            cache[{}] = checked ? true : false;
                            updateProgress();
                            localStorage.setItem("xiaobaiyue.markers", JSON.stringify(cache));
                            """.format(
                                    x["OSM"], x["OSM"], x["OSM"], x["OSM"]
                                ),
                            )
                        with a.td(klass="center"):
                            a(str(x["id-2017"] or "-"))
                        with a.td(klass="center"):
                            a(str(x["id-2006"] or "-"))
                        with a.td(klass="center"):
                            a(str(x["id-2003"] or "-"))
                        with a.td(klass="header"):
                            a.span(id="chinese_" + str(x["OSM"]), _t=x["chinese"])
                            a.button(
                                klass="btn",
                                **{
                                    "data-clipboard-target": "#chinese_" + str(x["OSM"])
                                },
                                _t="Copy"
                            )
                        with a.td(
                            klass="link",
                            onClick='$("#map")[0].scrollIntoView();map.flyTo([{}], 15);markers[{}].openPopup()'.format(
                                x["location"], x["OSM"]
                            ),
                        ):
                            a(x["english"])
                        with a.td():
                            a("{}m".format(round(float(x["height"]))));
                        with a.td(id="location"):
                            if x["location"] != None:
                                coords = str(x["location"]).split(",")
                            a.span(
                                id="location_" + str(x["OSM"]),
                                _t="{}, {}".format(
                                    round(float(coords[0]), 3),
                                    round(float(coords[1]), 3),
                                ),
                            )
                            a.button(
                                klass="btn",
                                **{
                                    "data-clipboard-target": "#location_"
                                    + str(x["OSM"])
                                },
                                _t="Copy"
                            )
print(str(a))

