// Map settings
const MAP_LAYERS = [{url: 'https://tile.happyman.idv.tw/map/moi_osm/{z}/{x}/{y}.png', attribution: ""}, { url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', attribution: '© OpenStreetMap' }]
const DEFAULT_COORDINATES = [23.9739881, 120.9097797]
const DEFAULT_ZOOM = 7
const MAX_ZOOM = 19

// Colors for progress bar
const GRADIENT = {
  100: "#57bb8a",
  95: "#63b682",
  90: "#73b87e",
  85: "#84bb7b",
  80: "#94bd77",
  75: "#a4c073",
  70: "#b0be6e",
  65: "#c4c56d",
  60: "#d4c86a",
  55: "#e2c965",
  50: "#f5ce62",
  45: "#f3c563",
  40: "#e9b861",
  35: "#e6ad61",
  30: "#ecac67",
  25: "#e9a268",
  20: "#e79a69",
  15: "#e5926b",
  10: "#e2886c",
  5: "#e0816d",
  0: "#dd776e"
}

// Map markers
var markers = {};
var layers = [];
const GREEN_ICON = L.AwesomeMarkers.icon({
  markerColor: 'green',
  prefix: 'fa',
  icon: 'mountain'
});
const RED_ICON = L.AwesomeMarkers.icon({
  markerColor: 'red',
  prefix: 'fa',
  icon: 'mountain'
});

// Load visited peaks from local storage.
var cache = JSON.parse(localStorage.getItem('xiaobaiyue.markers')) || {};

// Update the progress bar.
function updateProgress() {
  var climbed = 0;
  for (const [key, value] of Object.entries(cache)) {
    if (value) {
      climbed++;
    }
  }
  var percent = 100 * (climbed / TOTAL);
  var label = percent.toFixed(2) + "%";
  $("#progress").text(label);
  $("#progress").css("width", label);
  $("#progress").css("display", percent > 0 ? "block" : "none");
  $("#progress").css("padding-left", document.getElementById("progress").offsetWidth / 2 - getTextWidth(label, getCanvasFont(document.getElementById("progress")) / 2) + "px");
  $("#progress").css("background-color", GRADIENT[roundNearest5(percent)]);
}

$(document).ready(function () {
  // Add copy to clipboard function to buttons.
  new ClipboardJS('.btn');

  // Set the background of the description box.
  $("#description").vegas({
    "slides": [{
      src: "qixingshan.jpg"
    }]
  });
  
  $("#layer-selector").change(function() { 
    layers.forEach(layer => layer.removeFrom(map));
    layers[$("#layer-selector").val()].addTo(map);
  });
  
  $( window ).resize(function() {
    $("#progress").css("padding-left", document.getElementById("progress").offsetWidth / 2 - getTextWidth($("#progress").text(), getCanvasFont(document.getElementById("progress")) / 2) + "px");
  });

  // Set checkboxes based on cached values.
  for (const [key, value] of Object.entries(cache)) {
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
  var table = $("#xiaobaiyue").DataTable({
    paging: false,
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
        "width": "25%"
      },
      {
        "type": "html",
        "width": "40%"
      },
      {
        "type": "natural",
        "width": "10%"
      },
      {
        "orderable": false,
        "width": "10%"
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

// Utility functions
function roundNearest5(num) {
  return Math.round(num / 5) * 5;
}

function getTextWidth(text, font) {
  const canvas = getTextWidth.canvas || (getTextWidth.canvas = document.createElement("canvas"));
  const context = canvas.getContext("2d");
  context.font = font;
  const metrics = context.measureText(text);
  return metrics.width;
}

function getCssStyle(element, prop) {
  return window.getComputedStyle(element, null).getPropertyValue(prop);
}

function getCanvasFont(el = document.body) {
  const fontWeight = getCssStyle(el, 'font-weight') || 'normal';
  const fontSize = getCssStyle(el, 'font-size') || '16px';
  const fontFamily = getCssStyle(el, 'font-family') || 'Times New Roman';

  return `${fontWeight} ${fontSize} ${fontFamily}`;
}
