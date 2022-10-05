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
      html: '<ul><li><a href="#" onClick="swal.close();jumpTo(\'about\');">About</a></li><li><a href="mailto:xiaobaiyue@5164.at">Contact</a></li><li>❤️ <a href="https://coindrop.to/xiaobaiyue">Give a tip</a></li></ul>',
      showCloseButton: true,
      showConfirmButton: false
    })
  });

  setTimeout(function() {
    if (!localStorage['landscape.ack'] && window.innerHeight > window.innerWidth) {
      Swal.fire({
        icon: 'info',
        html: '<p class="center">This page displays more details in landscape mode.</p>',
        showCloseButton: true,
        showConfirmButton: false
      }).then((result) => {
        localStorage.setItem('landscape.ack', true);
      })
    }
  }, POPUP_DELAY);
  
  $("#accordion").accordion({
    heightStyle: "content",
    collapsible: true,
    active: false,
    icons: false
  });

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

function addMarker(osm, lon, lat, type, chinese, english, elevation) {
  var notVisitedIcon = type == "百岳" ? 百岳_ICON : 小百岳_ICON;
  var visitedIcon = type == "百岳" ? 百岳_VISITED_ICON : 小百岳_VISITED_ICON;
  var displayFunction = type == "百岳" ? "displayBaiyue()" : "displayXiaobaiyue()";
  var hikingBijiCategory = type =="百岳" ? 1 : 2;
  markers[osm] = L.marker([lon, lat], {icon: $("#"+osm).prop("checked") ? visitedIcon : notVisitedIcon}).bindPopup('<h2><a onClick="' + displayFunction + ';jumpTo(' + osm + ');">' + chinese + ' ' + english + ' ' + elevation +'m' + '</a></h2><ul><li><button class="btn ui-button ui-widget ui-corner-all" data-clipboard-text="' + lon + ', ' + lat +'">Copy location (WGS84)</button></li><li><a class="btn ui-button ui-widget ui-corner-all" href="https://hiking.biji.co/index.php?q=mountain&category='+ hikingBijiCategory + '&page=1&keyword=' + chinese + '" target="_blank">健行筆記</a>&nbsp;<a class="btn ui-button ui-widget ui-corner-all" target="_blank" href="https://www.google.com/maps/place/' + lon+','+lat +'">Google Maps</a></li></ul>').addTo(map);
}

