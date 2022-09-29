// Number of peaks
const TOTAL_XIAOBAIYUE = 116;
const TOTAL_BAIYUE = 100;

// Map settings
const MAP_LAYERS = [{url: 'https://tile.happyman.idv.tw/map/moi_osm/{z}/{x}/{y}.png', attribution: ""}, { url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', attribution: '© OpenStreetMap' }]
const DEFAULT_COORDINATES = [23.9739881, 120.9097797]
const DEFAULT_ZOOM = 7
const MAX_ZOOM = 19

// Delay after which a (one-time) notice is shown that the page looks better in landscape mode
const POPUP_DELAY = 30000

// Map markers
var markers = {};
var layers = [];
const 百岳_VISITED_ICON = L.AwesomeMarkers.icon({
  markerColor: 'blue',
  prefix: 'fa',
  icon: 'mountain'
});
const 小百岳_VISITED_ICON = L.AwesomeMarkers.icon({
  markerColor: 'green',
  prefix: 'fa',
  icon: 'mountain'
});
const 百岳_ICON = L.AwesomeMarkers.icon({
  markerColor: 'darkblue',
  prefix: 'fa',
  icon: 'mountain'
});
const 小百岳_ICON = L.AwesomeMarkers.icon({
  markerColor: 'darkgreen',
  prefix: 'fa',
  icon: 'mountain'
});

// Load visited peaks from local storage.
var baiyueMarkers = JSON.parse(localStorage.getItem('baiyue.markers')) || {};
var xiaobaiyueMarkers = JSON.parse(localStorage.getItem('xiaobaiyue.markers')) || {};

// Update the progress bar.
function updateProgress() {
  var baiyueClimbed = 0;
  for (const [key, value] of Object.entries(baiyueMarkers)) {
    if (value) {
      baiyueClimbed++;
    }
  }
  var xiaobaiyueClimbed = 0;
  for (const [key, value] of Object.entries(xiaobaiyueMarkers)) {
    if (value) {
      xiaobaiyueClimbed++;
    }
  }
  $("label[for='baiyue-checkbox']").text("Baiyue: " + baiyueClimbed + "/" + TOTAL_BAIYUE);
  $("label[for='xiaobaiyue-checkbox']").text("Xiaobaiyue: " + xiaobaiyueClimbed + "/" + TOTAL_XIAOBAIYUE);
}

$(document).ready(function () {
  $("a#menu").click(function() {
    Swal.fire({
      title: 'Taiwan\'s 百岳 Baiyue',
      html: '<ul><li><a href="#" onClick="showFaq()">FAQ</a></li><li><a href="mailto:xiaobaiyue@5164.at">Contact</a></li><li>❤️ <a href="https://coindrop.to/xiaobaiyue">Give a tip</a></li></ul>',
      showCloseButton: true,
      showConfirmButton: false
    })
  });

  setTimeout(function() {
    if (!localStorage['landscape.ack'] && window.innerHeight > window.innerWidth) {
      Swal.fire({
        icon: 'info',
        html: '<p>This page displays more details in landscape mode.</p>',
        showCloseButton: true,
        showConfirmButton: false
      })
    }
  }, POPUP_DELAY);

  // Add copy to clipboard function to buttons.
  new ClipboardJS('.btn');

  // Set the background of the description box.
  $("#description").vegas({
    delay: 10000,
    slides: [
        { src: "qixingshan.jpg" },
        { src: "yushan.jpg" },
    ]
  });
  
  $("#layer-selector").change(function() { 
    layers.forEach(layer => layer.removeFrom(map));
    layers[$("#layer-selector").val()].addTo(map);
  });
  
  $("#baiyue-checkbox, #xiaobaiyue-checkbox").change(function() { 
    updateMarkers();
  });
  
  $("label[for='baiyue-checkbox'],label[for='xiaobaiyue-checkbox']").on("click", function(e) { 
    updateMarkers();
    e.stopPropagation();
  });
  
  // Set checkboxes based on cached values.
  for (const [key, value] of Object.entries(baiyueMarkers)) {
    $("#" + key).prop("checked", value);
  }
  for (const [key, value] of Object.entries(xiaobaiyueMarkers)) {
    $("#" + key).prop("checked", value);
  }

  updateProgress();

  // Initialize map.
  map = L.map("map", {
    attributionControl: true,
    scrollWheelZoom: false,
    dragging: !L.Browser.mobile
  }).setView(DEFAULT_COORDINATES, DEFAULT_ZOOM);
  MAP_LAYERS.forEach((layer, index) => layers[index] = L.tileLayer(layer["url"], { maxZoom: MAX_ZOOM, attribution: layer["attribution"] }));
  layers[$("#layer-selector").val() || 0].addTo(map);
  map.attributionControl.setPrefix("");  
  
  $("#baiyue").DataTable({
    paging: false,
    info: false,
    responsive: {
      details: false
    },
    order: [
      [1, 'asc']
    ],
    "columns": [{
        "orderable": false,
        "width": "5%"
      },
      {
        "type": "natural",
        "width": "5%"
      },
      {
        "type": "html",
        "width": "27%"
      },
      {
        "type": "html",
        "width": "33%"
      },
      {
        "type": "natural",
        "width": "10%"
      },
      {
        "orderable": false,
        "width": "20%"
      }
    ],
  });
  $("#xiaobaiyue").DataTable({
    paging: false,
    info: false,
    responsive: {
      details: false
    },
    order: [
      [1, 'asc']
    ],
    "columns": [{
        "orderable": false,
        "width": "3%"
      },
      {
        "type": "natural",
        "width": "4%"
      },
      {
        "type": "natural",
        "width": "4%"
      },
      {
        "type": "natural",
        "width": "4%"
      },
      {
        "type": "html",
        "width": "22%"
      },
      {
        "type": "html",
        "width": "33%"
      },
      {
        "type": "natural",
        "width": "10%"
      },
      {
        "orderable": false,
        "width": "20%"
      }
    ],
  });
  
  // Initialize the center of the map and the zoom level.
  var lat = localStorage['xiaobaiyue.lat'] || 23.9739881;
  var lng = localStorage['xiaobaiyue.lng'] || 120.9097797;
  var zoom = localStorage['xiaobiayue.zoom'] || 7;
  map.setView([lat, lng], zoom);

  // Store the current map center and zoom level in the local storage.
  map.on('zoomend', function (e) {
    localStorage['xiaobiayue.zoom'] = map.getZoom();
  });
  map.on('moveend', function (e) {
    localStorage['xiaobaiyue.lat'] = map.getCenter().lat;
    localStorage['xiaobaiyue.lng'] = map.getCenter().lng;
  });

  displayBaiyue();
});

function updateMarkers() {
  var baiyue = $("#baiyue-checkbox").is(":checked");
  var xiaobaiyue = $("#xiaobaiyue-checkbox").is(":checked");
  for (const [key, value] of Object.entries(markers)) {
    markers[key].removeFrom(map);
    if ((BAIYUE.includes(parseInt(key)) && baiyue) || (XIAOBAIYUE.includes(parseInt(key)) && xiaobaiyue)) {
      markers[key].addTo(map);
    }
  }
}

function displayBaiyue() {
  $("#tab-baiyue").css("display", "unset");
  $("#tab-xiaobaiyue").css("display", "none");
  $($.fn.dataTable.tables(true)).DataTable().responsive.recalc();
}

function displayXiaobaiyue() {
  $("#tab-baiyue").css("display", "none");
  $("#tab-xiaobaiyue").css("display", "unset");
  $($.fn.dataTable.tables(true)).DataTable().responsive.recalc();
}

function jumpTo(id) {
  $("html,body").animate({scrollTop: $("#"+id).offset().top},"slow");
}

function flyTo(lon, lat, osm) {
  $("#map")[0].scrollIntoView();
  map.flyTo([lon, lat], 15);
  markers[osm].openPopup();
}

function toggleVisited(type, osm) {
  var checkbox = $("#"+osm);
  var checked = $("#"+osm).prop("checked");
  var notVisitedIcon = type == "百岳" ? 百岳_ICON : 小百岳_ICON;
  var visitedIcon = type == "百岳" ? 百岳_VISITED_ICON : 小百岳_VISITED_ICON;
  markers[osm].setIcon(checked ? visitedIcon : notVisitedIcon);
  if (type == "百岳") {
    baiyueMarkers[osm] = checked ? true : false;
    localStorage.setItem("baiyue.markers", JSON.stringify(baiyueMarkers));
  }
  else {
    xiaobaiyueMarkers[osm] = checked ? true : false;
    localStorage.setItem("xiaobaiyue.markers", JSON.stringify(xiaobaiyueMarkers));
  }
  updateProgress();
}

function showFaq() {
  Swal.fire({
      width: '80%',
      customClass: {
        container: 'faq'
      },
      html: `<h1>Why this project?</h1>
<p>There are many online resources about hiking in Taiwan, but English-language information about the Xiaobaiyue is limited and often outdated or incomplete. I aim to create an easy-to-use bucket list that also provides a starting point to plan a trip to each peak.</p>

<h1>How to use this page?</h1>
<p>At the top of the page, you will find a map of Taiwan. Baiyue peaks are marked in blue, Xiaobaiyue in green. You can see how the Baiyue are situated in the Yushan, Xueshan and Central Mountain ranges, while the Xiaobaiyue cover all of Taiwan's territory.</p>
<p>If you click on one of the markers, additional information is displayed in a pop-up. From here, you can jump to Hiking Biji, Google Maps or copy the GPS coordinates (WSG-84) or Chinese name of the peak into the clipboard to use in other apps.</p>
<p>Below the map is a table of all 100 Baiyue and 116 Xiaobaiyue. You can use the corresponding buttons to switch between the two.</p>
<p>Use the check box next to a peak to mark it as climbed; the total count (in the left lower corner of the map) will update accordingly. You can jump back to the map where that peak will be highlighted by clicking the name of a mountain.</p>

<h1>Where is the progress stored? Is my date secure?</h1>
<p>All information Is stored client-side, which means locally in your browser. Hiking progress is never uploaded to any server.</p>
<p>Please note that this also means that your information will NOT synchronize between multiple devices, for example, your desktop and your phone.</p>

<h1>What is the history of the Baiyue?</h1>
<p>In 1964, Japanese Kyūya Fukada's published a successful book called 100 Famous Japanese Mountains. This influenced Wen-An Lin to compile a similar list of mountains in Taiwan. Together with other local mountaineers, 100 peaks known at the time to be above 3000 m were selected by criteria like uniqueness, danger, height, beauty and prominence.</p>
<p>The 百岳 Baiyue list was released in 1971 and has since become a bucket list for many Taiwanese hikers.</p>

<h1>What is the history of the Xiaobaiyue?</h1>
<p>In 1992, the Sports Committee of Taiwan identified 100 entry-level hikes to promote national mountaineering. These peaks are known as the 小百岳 Xiaobaiyue, Taiwan's 100 "little" peaks.</p>

<h1>Are all Baiyue over 3000 meters?</h1>
<p>They were supposed to be. However, 鹿山 Lushan has since been re-surveyed to be slightly below that height (2981 meters). It has been kept on the list, regardless. There has also been some confusion about whether 六順山 Liushunshan is above 3000 meters, but its latest surveying came in at 3009 meters.</p>

<h1>Why are there more than 100 Xiaobaiyue?</h1>
<p>In contrast to the Baiyue, the Xiaobaiyue list has been updated several times, and peaks have been replaced for various reasons (for example, difficulty of access). This page displays all versions of the list (at least all that I could find so far).</p>
<p>If you're only interested in the latest one, focus on the ones with a number in the 2017 column and disregard the 16 old ones. That said, some former peaks are still beautiful hikes.</p>

<h1>Where does the data come from?</h1>
<p>I used the Chinese Wikipedia pages for the Baiyue and Xiaobaiyue and some of the primary sources mentioned there. I also consulted the following books: Richard Saunders: Taiwan 101: Essential sights, hikes and experiences on Ilha Formosa Vol. 1 and 2, 台灣小百岳．走遍全台１００登山輕旅行.
<p>Elevation data is taken from <a href="https://openstreetmap.org">OpenStreetMap (OSM)</a>.</p>

<h1>How are the English translations of the peaks chosen?</h1>
<p>The naming pattern uses the Hanyu Pinyin transliteration of the Chinese name while keeping 山 shan untranslated, for example, <em>Qixingshan</em> instead of Mt. Qixing, Mt. Cising, Mt. Chihsing or Seven Star Mountain.</p>
<p>If there are multiple peaks (North, South, East, West etc.), that distinction is translated into English, such as <em>Yushan Front Peak</em>.</p>

<h1>Are you planning to add route descriptions for each peak?</h1>
<p>Maybe.</p>
<p>The challenge is that it would be difficult to keep this information accurate and up to date, especially given that there are many different route combinations for most peaks (and I wouldn't want to select just one). Ultimately, a community-maintained platform seems to be a better place to find route descriptions. For this reason, I link to <a href="https://hiking.biji.co/">Hiking Biji</a> entry for most peaks.</p>

<h1>How can I report an error or a problem?</h1>
<p>
  <a href="mailto:xiaobaiyue@5164.at" target="_blank">E-mail me</a>. I&rsquo;m looking forward to hearing from you.
</p>

<h1>Have you climbed all the peaks?</h1>
<p>Not yet, but I&rsquo;m enjoying the journey.</p>

<h1>What programming languages did you use?</h1>
<p>Some Python and a bit of Javascript.</p>

<h1>How is this project paid for?</h1>
<p>This is a hobby project. If you find it useful, you can <a href="https://coindrop.to/xiaobaiyue">give a tip</a>, but there is no obligation.</p>`,
      showCloseButton: true,
      showCancelButton: true,
      cancelButtonText: 'Close',
      showConfirmButton: false
    });
}

function addMarker(osm, lon, lat, type, chinese, english, elevation) {
  var notVisitedIcon = type == "百岳" ? 百岳_ICON : 小百岳_ICON;
  var visitedIcon = type == "百岳" ? 百岳_VISITED_ICON : 小百岳_VISITED_ICON;
  var displayFunction = type == "百岳" ? "displayBaiyue()" : "displayXiaobaiyue()";
  var hikingBijiCategory = type =="百岳" ? 1 : 2;
  markers[osm] = L.marker([lon, lat], {icon: $("#"+osm).prop("checked") ? visitedIcon : notVisitedIcon}).bindPopup('<h2><a onClick="' + displayFunction + ';jumpTo(' + osm + ');">' + chinese + ' ' + english + ' ' + elevation +'m' + '</a></h2><ul><li><button class="btn ui-button ui-widget ui-corner-all" data-clipboard-text="' + lon + ', ' + lat +'">Copy location (WGS84)</button></li><li><a class="btn ui-button ui-widget ui-corner-all" href="https://hiking.biji.co/index.php?q=mountain&category='+ hikingBijiCategory + '&page=1&keyword=' + chinese + '" target="_blank">健行筆記</a>&nbsp;<a class="btn ui-button ui-widget ui-corner-all" target="_blank" href="https://www.google.com/maps/place/' + lon+','+lat +'">Google Maps</a></li></ul>').addTo(map);
}

