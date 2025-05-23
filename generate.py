#!/usr/bin/python
# -*- coding: utf-8 -*-

import yaml
from yaml.loader import SafeLoader
from airium import Airium

with open("data.yml") as f:
    data = yaml.load(f, Loader=SafeLoader)
    markers = ""
    constants = ""
    baiyue = []
    xiaobaiyue = []
    oldXiaobaiyue = []
    for x in data:
        baiyue.append(x["OSM"]) if x["type"] == "百岳" else xiaobaiyue.append(x["OSM"]) if x["id-2017"] else oldXiaobaiyue.append(x["OSM"])
        markers += 'addMarker({}, {}, "{}", "{}", "{}", "{}", "{}", "{}", "{}");'.format(
            x["OSM"],
            x["location"],
            x["type"] if x["type"] == "百岳" or x["id-2017"] else "小百岳_OLD",
            x["id"] if x["type"] == "百岳" else str(x["id-2017"] or ""),
            x["chinese"],
            x["english"],
            round(x["elevation"]),
            x["region"],
            x.get("descriptions")
        )
    constants += "const BAIYUE={};const XIAOBAIYUE={};const OLD_XIAOBAIYUE={};".format(baiyue, xiaobaiyue, oldXiaobaiyue)
a = Airium()
a("<!DOCTYPE html>")
with a.html(lang="en"):
    with a.head():
        a.meta(charset="utf-8")
        a.meta(content="width=device-width, initial-scale=1", name="viewport")
        a.title(_t="Taiwan's 百岳 Baiyue")
        a.link(
            href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.min.css",
            rel="stylesheet",
        )
        a.link(
            href="https://cdn.datatables.net/responsive/2.3.0/css/responsive.dataTables.min.css",
            rel="stylesheet",
        )
        a.link(
            href="https://code.jquery.com/ui/1.13.1/themes/base/jquery-ui.min.css",
            rel="stylesheet",
        )
        a.link(
            href="https://unpkg.com/leaflet@1.8.0/dist/leaflet.css", rel="stylesheet"
        )
        a.link(
            href="https://cdnjs.cloudflare.com/ajax/libs/Leaflet.awesome-markers/2.0.2/leaflet.awesome-markers.css",
            rel="stylesheet",
        )
        a.link(href="https://api.mapbox.com/mapbox.js/plugins/leaflet-fullscreen/v1.0.1/leaflet.fullscreen.css", rel="stylesheet")
        a.link(href="https://jjimenezshaw.github.io/Leaflet.Control.Resizer/L.Control.Resizer.css", rel="stylesheet")
        a.link(href="https://cdnjs.cloudflare.com/ajax/libs/lightbox2/2.11.4/css/lightbox.min.css", rel="stylesheet")
        a.link(href="style.css", rel="stylesheet")
        a.link(href="favicon.png", rel="icon")
        a.meta(content="chrome=1", **{"http-equiv": "X-UA-Compatible"})
        a.meta(content="True", name="HandheldFriendly")
        a.meta(content="320", name="MobileOptimized")
        a.meta(content="width=device-width, initial-scale=1.0", name="viewport")
        a.meta(content="no-referrer", name="referrer")
        a.meta(content="M.S.", name="author")
        a.meta(content="Taiwan's 百岳 Baiyue", name="description")
        a.script(src="script.js")
        with a.script():

            a(
                "{}function initMarkers(){{{}}};".format(
                    constants, markers
                )
            )
    with a.body():
        with a.div(klass="container"):
            with a.div(id="map"):
                with a.div(klass="leaflet-top leaflet-right"):
                    with a.select(id="dropdown-menu", onChange="menuEvent(value);"):
                        a.option(_t="≡", value="0", klass="hidden", selected=True, disabled=True)
                        a.option(id="reset-map", value="1", _t="TAIWAN"),
                        a.option(id="focus-north", value="2", _t="Northern Taiwan"),
                        a.option(id="focus-central", value="3", _t="Central Taiwan"),
                        a.option(id="focus-south", value="4", _t="Southern Taiwan"),
                        a.option(id="focus-east", value="5", _t="Eastern Taiwan"),
                        a.option(id="focus-islands", value="6", _t="Outlying Islands"),
                        a.option(id="focus-location", value="7", _t="Current location"),                        
                        a.option(_t="──────────", disabled=True),
                        a.option(id="backup-progress", value="8",_t="Backup to file"),
                        a.option(id="restore-progress", value="9",_t="Restore from file"),
                        a.option(id="reset-progress", value="10",_t="Delete my data"),
                        a.option(value="11",_t="About")
                with a.div(klass="leaflet-bottom leaflet-left"):
                    with a.div("checkboxes"):
                        a.input(id="baiyue-checkbox", type="checkbox", checked=True)
                        a.label(_t="百岳", **{"for": "baiyue-checkbox"})
                    with a.div("checkboxes"):
                        a.input(id="xiaobaiyue-checkbox", type="checkbox", checked=True)
                        a.label(_t="小百岳", **{"for": "xiaobaiyue-checkbox"})

        with a.div(id="table-container"):
            with a.div(klass="btn-box"):
                a.a(
                    _t="百岳 Baiyue",
                    id="baiyue-btn",
                    klass="ui-button ui-widget ui-corner-all",
                    onClick="displayBaiyue()",
                )
                a.a(
                    _t="小百岳 Xiaobaiyue",
                    id="xiaobaiyue-btn",
                    klass="ui-button ui-widget ui-corner-all",
                    onClick="displayXiaobaiyue()",
                )
            with a.div(id="tab-baiyue"):
                with a.table(klass="display", id="baiyue", width="100%"):
                    with a.thead():
                        with a.tr():
                            a.th(_t="✔️", **{"data-priority": "2"}, klass="center")
                            a.th(_t="#", **{"data-priority": "3"}, klass="center")
                            a.th(_t="Chinese name", **{"data-priority": "2"})
                            a.th(_t="English name", **{"data-priority": "1"})
                            a.th(_t="Elevation", **{"data-priority": "4"})
                            a.th(_t="Location", **{"data-priority": "5"})
                    with a.tbody():
                        for x in data:
                            if not x["type"] == "百岳":
                                continue
                            with a.tr():
                                with a.td(klass="center"):
                                    a.a(name="{}".format(str(x["OSM"])))
                                    a.input(
                                        type="checkbox",
                                        id="{}".format(x["OSM"]),
                                        onClick='toggle("{}",{});'.format(
                                            x["type"], x["OSM"]
                                        ),
                                    )
                                with a.td(klass="center"):
                                    a(str(x["id"] or "-"))
                                with a.td():
                                    a.span(
                                        id="chinese_" + str(x["OSM"]), _t=x["chinese"]
                                    )
                                    a.button(
                                        klass="btn ui-button ui-widget ui-corner-all",
                                        **{
                                            "data-clipboard-target": "#chinese_"
                                            + str(x["OSM"])
                                        },
                                        _t="Copy"
                                    )
                                with a.td(
                                    klass="link",
                                    onClick="flyTo({},{})".format(
                                        x["location"], x["OSM"]
                                    ),
                                ):
                                    a(x["english"])
                                with a.td():
                                    a("{}m".format(round(float(x["elevation"]))))
                                with a.td(klass="location"):
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
                                        klass="btn ui-button ui-widget ui-corner-all",
                                        **{
                                            "data-clipboard-target": "#location_"
                                            + str(x["OSM"])
                                        },
                                        _t="Copy"
                                    )
            with a.div(id="tab-xiaobaiyue"):
                with a.table(klass="display", id="xiaobaiyue", width="100%"):
                    with a.thead():
                        with a.tr():
                            a.th(_t="✔️", **{"data-priority": "2"}, klass="center")
                            a.th(_t="2017", **{"data-priority": "3"}, klass="center")
                            a.th(_t="2006", klass="center")
                            a.th(_t="2003", klass="center")
                            a.th(_t="Chinese name", **{"data-priority": "2"})
                            a.th(_t="English name", **{"data-priority": "1"})
                            a.th(_t="Elevation", **{"data-priority": "4"})
                            a.th(_t="Location", **{"data-priority": "5"})
                    with a.tbody():
                        for x in data:
                            if not x["type"] == "小百岳":
                                continue
                            with a.tr():
                                with a.td(klass="center"):
                                    a.a(name="{}".format(str(x["OSM"])))
                                    type = x["type"];
                                    if not x["id-2017"]:
                                        type = type + "_OLD"
                                    a.input(
                                        type="checkbox",
                                        id="{}".format(x["OSM"]),
                                        onClick='toggle("{}",{});'.format(
                                            type, x["OSM"]
                                        ),
                                    )
                                with a.td(klass="center"):
                                    a(str(x["id-2017"] or "-"))
                                with a.td(klass="center"):
                                    a(str(x["id-2006"] or "-"))
                                with a.td(klass="center"):
                                    a(str(x["id-2003"] or "-"))
                                with a.td():
                                    a.span(
                                        id="chinese_" + str(x["OSM"]), _t=x["chinese"]
                                    )
                                    a.button(
                                        klass="btn ui-button ui-widget ui-corner-all",
                                        **{
                                            "data-clipboard-target": "#chinese_"
                                            + str(x["OSM"])
                                        },
                                        _t="Copy"
                                    )
                                with a.td(
                                    klass="link",
                                    onClick="flyTo({},{})".format(
                                        x["location"], x["OSM"]
                                    ),
                                ):
                                    a(x["english"])
                                with a.td():
                                    a("{}m".format(round(float(x["elevation"]))))
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
                                        klass="btn ui-button ui-widget ui-corner-all",
                                        **{
                                            "data-clipboard-target": "#location_"
                                            + str(x["OSM"])
                                        },
                                        _t="Copy"
                                    )
            a.a(id="about", name="about")
            with a.div(id="accordion"):
                a.h3(_t="What is the purpose of this project?")
                with a.div():
                    with a.p():
                        a(
                            "There are many online resources about hiking in Taiwan, but English-language information (especially about the Xiaobaiyue) is still limited and often incomplete. This project aims to create an easy-to-use bucket list that also provides a starting point to plan a trip to each 百岳 Baiyue and 小百岳 Xiaobaiyue."
                        )
                a.h3(_t="How to use this page?")
                with a.div():
                    a.p(
                        _t="At the top of the page, you will find a map of Taiwan. 百岳 peaks are marked in blue, 小百岳 in green."
                    )
                    a.img(src="screencast1.gif")
                    a.p(
                        _t="If you click on one of the markers, additional information is displayed. From here, you can jump to online maps and route descriptions. You can also copy the GPS coordinates (WSG-84) or the Chinese name of the peak into the clipboard."
                    )
                    a.p(
                        _t="You can mark a peak as visited and, optionally, attach your summit photo too. As detailled below, all data (including the photos) is only stored locally in your browser and never uploaded anywhere!"
                    )
                    a.p(
                        _t="Below the map is a table of all mountains. You can use the corresponding buttons to switch between a list of 百岳 and 小百岳."
                    )
                    a.p(
                        _t="Use the check box next to a peak to mark it as climbed. The total count (in the left lower corner of the map) and the icon for that peak will update accordingly.")
                a.h3(_t="Where is the progress stored? Is my data secure?")
                with a.div():
                    with a.p():
                        a("All information is stored client-side, which means locally in your browser. Hiking progress and photos are never uploaded to any server.")
                    with a.p():
                        a("Please note that this also means that your information won't synchronize between multiple devices, for example, your desktop and your phone. You can use the backup & restore functions to copy your data between devices."
                        )
                a.h3(_t="What is the history of the 百岳?")
                with a.div():
                    a.p(
                        _t='The 1964 book “100 Famous Japanese Mountains“ influenced Taiwanese hiking legend Wen-An Lin to compile a similar list of mountains in Taiwan. Together with other local mountaineers, he selected 100 peaks known at the time to be above 3000 m. The peaks were chosen by criteria like uniqueness, danger, height, beauty and prominence.'
                    )
                    a.p(
                        _t="The 百岳 list was released in 1971 and has since become a bucket list for many Taiwanese hikers."
                    )
                a.h3(_t="What is the history of the 小百岳?")
                with a.div():
                    a.p(
                        _t='The Sports Committee of Taiwan identified 100 entry-level hikes to promote national mountaineering. These peaks are known as the 小百岳, Taiwan\'s 100 "little" peaks and a first list was released in 2003.'
                    )
                a.h3(_t="Are all 百岳 over 3000 meters?")
                with a.div():
                    a.p(
                        _t='They were supposed to be. However, 鹿山 Lushan has since been re-surveyed to be "only" 2981 meters high. It has been kept on the list, regardless.'
                    )
                a.h3(_t="Why are there more than 100 小百岳?")
                with a.div():
                    a.p(
                        _t="In contrast to the Baiyue, the Xiaobaiyue list has been updated several times, and peaks have been replaced for various reasons (for example, difficulty of access). This page displays all versions of the list."
                    )
                a.h3(_t="Where does the data come from?")
                with a.div():
                    with a.p():
                        a("The information has been taken from the Chinese Wikipedia pages for the")
                        a.a(href="https://zh.wikipedia.org/wiki/%E5%8F%B0%E7%81%A3%E7%99%BE%E5%B2%B3", _t="百岳")
                        a("and")
                        a.a(href="https://zh.wikipedia.org/zh-hant/%E5%8F%B0%E7%81%A3%E5%B0%8F%E7%99%BE%E5%B2%B3%E5%88%97%E8%A1%A8", _t="小百岳")
                        a("and the primary sources mentioned there. I also consulted Richard Saunders' books on Taiwan and 「台灣小百岳．走遍全台１００登山輕旅行」.")
                    with a.p():
                        a("Elevation data is taken from")
                        a.a(
                            href="https://openstreetmap.org",
                            _t="OpenStreetMap (OSM)",
                        )
                        a(".")
                a.h3(_t="How are the English translations of the peaks chosen?")
                with a.div():
                    with a.p(
                        _t="The naming pattern uses the Hanyu Pinyin transliteration of the Chinese name while keeping 山 shan untranslated, for example, 七星山 is written as"
                    ):
                      a.em(_t="Qixingshan")
                      a(
                        "instead of Mt. Qixing, Mt. Cising, Mt. Chihsing or Seven Star Mountain."
                    )
                    with a.p(
                        _t="If there are multiple peaks (North, South, East, West etc.), that distinction is translated into English, such as"
                    ):
                      a.em(_t="Yushan East Peak")
                      a(".")
                a.h3(_t="How can I report an error or a problem?")
                with a.div():
                    with a.p():
                        a.a(
                            href="mailto:xiaobaiyue@5164.at",
                            target="_blank",
                            _t="E-mail me",
                        )
                        a(". I’m looking forward to hearing from you.")
                a.h3(_t="Have you climbed all the peaks?")
                with a.div():
                    a.p(_t="No, but I’m enjoying the journey.")
                a.h3(_t="What programming languages did you use?")
                with a.div():
                    a.p(_t="Some Python and a bit of Javascript.")
                a.h3(_t="How is this project paid for?")
                with a.div():
                    with a.p():
                        a("This is a hobby project. If you find it useful, you can")
                        a.a(href="https://coindrop.to/xiaobaiyue", _t="give a tip")
                        a(", but there is no obligation.")
            with a.div(klass="btn-box"):
                a.a(
                    klass="btn ui-button ui-widget ui-corner-all",
                    onclick="$(window).scrollTop(0);",
                    _t="Top",
                )
print(str(a))
