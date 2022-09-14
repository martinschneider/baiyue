// Number of peaks
const TOTAL_XIAOBAIYUE = 116;
const TOTAL_BAIYUE = 100;

// Map settings
const MAP_LAYERS = [{url: 'https://tile.happyman.idv.tw/map/moi_osm/{z}/{x}/{y}.png', attribution: ""}, { url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', attribution: '© OpenStreetMap' }]
const DEFAULT_COORDINATES = [23.9739881, 120.9097797]
const DEFAULT_ZOOM = 7
const MAX_ZOOM = 19

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
var xiaoBaiyueMarkers = JSON.parse(localStorage.getItem('xiaobaiyue.markers')) || {};

// Update the progress bar.
function updateProgress() {
  var baiyueClimbed = 0;
  for (const [key, value] of Object.entries(baiyueMarkers)) {
    if (value) {
      baiyueClimbed++;
    }
  }
  var xiaoBaiyueClimbed = 0;
  for (const [key, value] of Object.entries(xiaoBaiyueMarkers)) {
    if (value) {
      xiaoBaiyueClimbed++;
    }
  }
  $("#progress").text("Baiyue: " + baiyueClimbed + "/" + TOTAL_BAIYUE + ", Xiaobaiyue: " + xiaoBaiyueClimbed + "/" + TOTAL_XIAOBAIYUE);
}

$(document).ready(function () {
  // Add copy to clipboard function to buttons.
  new ClipboardJS('.btn');

  // Set the background of the description box.
  $("#description").vegas({
    slides: [
        { src: 'qixingshan.jpg' },
    ]
  });
  
  $("#layer-selector").change(function() { 
    layers.forEach(layer => layer.removeFrom(map));
    layers[$("#layer-selector").val()].addTo(map);
  });
  
  // Set checkboxes based on cached values.
  for (const [key, value] of Object.entries(baiyueMarkers)) {
    $("#" + key).prop("checked", value);
  }
  for (const [key, value] of Object.entries(xiaoBaiyueMarkers)) {
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
});

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
