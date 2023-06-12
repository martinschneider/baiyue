// Number of peaks
const TOTAL_XIAOBAIYUE = 100;
const TOTAL_OLD_XIAOBAIYUE = 16;
const TOTAL_BAIYUE = 100;

// Map settings
const MAP_LAYERS = [{
  url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: '© OpenStreetMap'
}];
const DEFAULT_COORDINATES = [23.9739881, 120.9097797];
const DEFAULT_ZOOM = 7;
const FLY_TO_ZOOM = 13;
const MAX_ZOOM = 19;

// Menu entries
const MENU_TAIWAN = 1;
const MENU_NORTH = 2;
const MENU_CENTRAL = 3;
const MENU_SOUTH = 4;
const MENU_EAST = 5;
const MENU_ISLANDS = 6;
const MENU_LOCATION = 7;
const MENU_BACKUP = 8;
const MENU_RESTORE = 9;
const MENU_RESET = 10;
const MENU_ABOUT = 11;

// Local storage keys
const ACTIVE_TAB = "activeTab";
const MAP_LAT = "mapLat";
const MAP_LNG = "mapLng";
const MAP_ZOOM = "mapZoom";
const CLIMBED_PEAKS = "climbed";
const HAS_PHOTO = "photos";

// Map markers
var markers = {};
var markerGroups = {
  "all": L.featureGroup(),
  "north": L.featureGroup(),
  "central": L.featureGroup(),
  "south": L.featureGroup(),
  "east": L.featureGroup(),
  "islands": L.featureGroup()
};
var layers = [];

const 百岳_VISITED_ICON = L.AwesomeMarkers.icon({
  markerColor: 'blue',
  prefix: 'fa',
  icon: 'check'
});
const 小百岳_VISITED_ICON = L.AwesomeMarkers.icon({
  markerColor: 'green',
  prefix: 'fa',
  icon: 'check'
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

const POSITION_ICON = L.AwesomeMarkers.icon({
  markerColor: 'red',
  prefix: 'fa',
  icon: 'person-hiking'
});

// Load visited peaks from local storage.
var climbed;

var currPosMarker;
var currPos;

$(document).ready(function() {
  localforage.config({
    driver: localforage.INDEXEDDB,
    name: 'baiyue',
    version: 1.0,
    storeName: 'baiyue',
    description: 'Climbing information and photos of peaks'
  });

  climbed = {};
  hasPhoto = new Set();

  localforage.getItem(CLIMBED_PEAKS).then(function(value) {
    climbed = value || {};
    updateProgress();
    updateCheckboxes();
    initMarkers();
  })
  
  localforage.getItem(HAS_PHOTO).then(function(value) {
    hasPhoto = new Set(Object.keys(value || {}));
  })

  // Initialize menu
  $("a#menu").click(function() {
    showMenu();
  });

  // Register accordion for FAQ section.
  $("#accordion").accordion({
    heightStyle: "content",
    collapsible: true,
    active: false,
    icons: false
  });

  // Add copy to clipboard function to buttons.
  new ClipboardJS('.btn');

  // Checkboxes should update the map markers.
  $("#baiyue-checkbox, #xiaobaiyue-checkbox").change(function() {
    toggleMarkers();
  });

  $("label[for='baiyue-checkbox'],label[for='xiaobaiyue-checkbox']").on("click", function(e) {
    toggleMarkers();
    e.stopPropagation();
  });

  // Initialize map.
  map = L.map("map", {
    attributionControl: true,
    scrollWheelZoom: false,
    fullscreenControl: true,
    zoomDelta: 0.5,
    zoomSnap: 0,
    dragging: !L.Browser.mobile
  });
  MAP_LAYERS.forEach((layer, index) => layers[index] = L.tileLayer(layer["url"], {
    maxZoom: MAX_ZOOM,
    attribution: layer["attribution"]
  }));
  layers[$("#layer-selector").val() || 0].addTo(map);
  map.attributionControl.setPrefix("");
  L.control.resizer({
    direction: "s",
    pan: "true"
  }).addTo(map);
  map.on("moveend zoomend", function() {
    // Store the current map center and zoom level in the local storage.
    localStorage[MAP_ZOOM] = map.getZoom();
    localStorage[MAP_LAT] = map.getCenter().lat;
    localStorage[MAP_LNG] = map.getCenter().lng;
  });
  navigator.geolocation.watchPosition(showPos);

  // Initialize data tables.
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

  if (localStorage.getItem(ACTIVE_TAB) == "xiaobaiyue") {
    displayXiaobaiyue();
  } else {
    displayBaiyue();
  }

  // Initialize the center of the map and the zoom level.
  var lat = localStorage[MAP_LAT] || DEFAULT_COORDINATES[0];
  var lng = localStorage[MAP_LNG] || DEFAULT_COORDINATES[1];
  var zoom = localStorage[MAP_ZOOM] || DEFAULT_ZOOM;
  map.setView([lat, lng], zoom);
});

function showPos(position) {
  currPos = position;
  if (!currPosMarker) {
    currPosMarker = L.marker([position.coords.latitude, position.coords.longitude], {icon: POSITION_ICON});
    currPosMarker.bindTooltip("Your current location").openTooltip();
    currPosMarker.addTo(map);
  }
  currPosMarker.setLatLng(new L.LatLng(position.coords.latitude, position.coords.longitude));
}

// Toggle between Xiaobaiyue and Baiyue map markers.
function toggleMarkers() {
  var baiyue = $("#baiyue-checkbox").is(":checked");
  var xiaobaiyue = $("#xiaobaiyue-checkbox").is(":checked");
  for (const [key, value] of Object.entries(markers)) {
    markers[key].removeFrom(map);
    if ((BAIYUE.includes(parseInt(key)) && baiyue) || ((XIAOBAIYUE.includes(parseInt(key)) || OLD_XIAOBAIYUE.includes(parseInt(key))) && xiaobaiyue)) {
      markers[key].addTo(map);
    }
  }
}

// Update the map markers.
function updateMarkers() {
  for (osm of BAIYUE) {
    markers[osm].setIcon($("#" + osm).prop("checked") ? 百岳_VISITED_ICON : 百岳_ICON);
  }
  for (osm of XIAOBAIYUE) {
    markers[osm].setIcon($("#" + osm).prop("checked") ? 小百岳_VISITED_ICON : 小百岳_ICON);
  }
  for (osm of OLD_XIAOBAIYUE) {
    markers[osm].setIcon($("#" + osm).prop("checked") ? 小百岳_VISITED_ICON : 小百岳_ICON);
  }
}

// Update progress bar.
function updateProgress() {
  var baiyueClimbed = 0;
  var xiaobaiyueClimbed = 0;
  var oldXiaobaiyueClimbed = 0;
  for (const [key, value] of Object.entries(climbed)) {
    if (value) {
      if (BAIYUE.includes(parseInt(key))) {
        baiyueClimbed++;
      } else if (XIAOBAIYUE.includes(parseInt(key))) {
        xiaobaiyueClimbed++;
      } else if (OLD_XIAOBAIYUE.includes(parseInt(key))) {
        oldXiaobaiyueClimbed++;
      }
    }
  }
  $("label[for='baiyue-checkbox']").text("百岳：" + baiyueClimbed + "/" + TOTAL_BAIYUE);
  $("label[for='xiaobaiyue-checkbox']").text("小百岳：" + xiaobaiyueClimbed + "/" + TOTAL_XIAOBAIYUE + "、座舊小百岳：" + oldXiaobaiyueClimbed + "/" + TOTAL_OLD_XIAOBAIYUE);
}

// Switch to the Baiyue tab.
function displayBaiyue() {
  $("#tab-baiyue").css("display", "unset");
  $("#tab-xiaobaiyue").css("display", "none");
  $($.fn.dataTable.tables(true)).DataTable().responsive.recalc();
  localStorage.setItem(ACTIVE_TAB, "baiyue");
}

// Switch to the Xiaobaiyue tab.
function displayXiaobaiyue() {
  $("#tab-baiyue").css("display", "none");
  $("#tab-xiaobaiyue").css("display", "unset");
  $($.fn.dataTable.tables(true)).DataTable().responsive.recalc();
  localStorage.setItem(ACTIVE_TAB, "xiaobaiyue");
}

// Scroll to a Baiyue or Xiaobaiyue in the table.
function jumpTo(id) {
  if (map.isFullscreen()) {
    map.toggleFullscreen();
  }
  $("html,body").animate({
    scrollTop: $("#" + id).offset().top - 50
  }, "slow");
  $("#" + id).closest("tr").addClass("highlight").delay(3000).queue(function() {
    $(this).removeClass("highlight").dequeue();
  });
}

// Scroll to the map, fly to the location of a peak and open its details pop-up.
function flyTo(lon, lat, osm) {
  $("#map")[0].scrollIntoView();
  map.flyTo([lon, lat], FLY_TO_ZOOM);
  markers[osm].openPopup();
  $(".buttonset").buttonset();
}

// Add a peak marker to the map.
function addMarker(osm, lon, lat, type, id, chinese, english, height, region, descriptions) {
  var displayFunction = type == "百岳" ? "displayBaiyue()" : "displayXiaobaiyue()";
  var hikingBiji = type != "小百岳_OLD";
  var hikingBijiCategory = type == "百岳" ? 1 : 2;
  var idStr = "";
  if (id) {
    idStr = "#" + id + " ";
  }
  var checked = $("#" + osm).prop("checked");
  var toggleLabel = checked ? "unvisited" : "visited";
  var popup = "<h2><a onClick=\"" + displayFunction + ";jumpTo(" + osm + ");\">" + idStr + chinese + " " + english + " (" + height + " m)</a></h2>"

  // Actions
  popup += "<div class=\"buttonset actionset\">"

  // Toggle climbing status
  popup += "<a class=\"btn\" id=\"toggleButton\" onClick=\"toggle(" + "'" + type + "', " + osm + ");\">Mark as " + toggleLabel + "</a>"
  
  // Photo
  popup += "<a class=\"btn\" onClick=\"uploadPhoto(" + "'" + type + "', " + osm + ")\">Photo</a>";
  popup += "</div>";

  // Navigation
  popup += "<div class=\"actionset\"><select class=\"btn ui-button\" onChange=\"actionEvent(this, this.value);\"><option value=\"0\" class=\"hidden\" selected=\"true\" disabled=\"true\">Navigation</option>";
  // Google Maps
  popup += "<option value=\"https://www.google.com/maps/place/" + lon +','+ lat +"\" title=\"Google Maps\">Google Maps</option>";
  // Organic Maps
  popup += "<option value=\"om://map?v=1&ll=" + lon + ", " + lat + "\" title=\"Organic Maps\">Organic Maps</option>";
  // Copy location
  popup += "<option value=\"" + lon + ", " + lat +"\">Copy location (WGS84)</option></select>";

  
  // Route description
  if (descriptions != null && descriptions != "undefined" && descriptions != "None" && descriptions != "") {
    popup += "<select class=\"btn ui-button\" onChange=\"actionEvent(this, this.value);\">";
    popup += "<option value=\"0\" class=\"hidden\" selected=\"true\" disabled=\"true\">Route description</option>";
    for (const descr of descriptions.split(",")) {
      idx = descr.indexOf(":")
      name = descr.substring(0, idx)
      link = descr.substr(idx + 1).trim()
      console.log(name + "   " + link)
      popup += "<option value=\"" + link +"\">" + name + "</option>"
    }
    popup += "</select>"
  }
  else if (hikingBiji) {
    popup += "<select class=\"btn ui-button\" onChange=\"actionEvent(this, this.value);\">";
    popup += "<option value=\"0\" class=\"hidden\" selected=\"true\" disabled=\"true\">Route description</option>";
    popup += "<option value=\"https://hiking.biji.co/index.php?q=mountain&category=" + hikingBijiCategory + "&page=1&keyword=" + chinese + "\" title=\"健行筆記 Hiking Biji\">健行筆記 Hiking Biji</option></select>";
  }
  popup += "</div>";

  var icon = L.AwesomeMarkers.icon({
    prefix: 'fa'
  });
  
  localforage.getItem("photo_" + osm).then(function(value) {
    icon.options.markerColor = type == "百岳" ? checked ? "blue" : "darkblue" : checked ? "green" : "darkgreen";
    icon.options.icon = checked ? value != null ? "photo" : "check" : "mountain";
  
    if (!markers[osm]) {
      markers[osm] = L.marker([lon, lat], {
        icon: icon
      }).bindPopup(popup).on('popupopen', function(popup) {
        $(".buttonset").buttonset();
        adjustToggleLabel(osm)
      }).addTo(map).addTo(markerGroups["all"]).addTo(markerGroups[region]);	
    }
    else {
      markers[osm].setIcon(icon);
    }
  });
}

function adjustToggleLabel(osm) {
  var checked = $("#" + osm).prop("checked");
  var toggleLabel = checked ? "unvisited" : "visited";
  $("#toggleButton").text("Mark as " + toggleLabel);
}

// Reset the value in the menu select element.
function resetDropdown() {
  $("#dropdown-menu").val(0);
}

// Reset the progress for both Baiyue and Xiaobaiyue peaks.
function resetProgress() {
  Swal.fire({
    title: "Are you sure?",
    text: "Deleting your climbing progress and photos cannot be undone.",
    icon: "warning",
    showCancelButton: true,
    confirmButtonColor: "#3085d6",
    cancelButtonColor: "#d33",
    confirmButtonText: "Yes"
  }).then((result) => {
    if (result.isConfirmed) {
      climbed = {};
      localforage.dropInstance();
      updateProgress();
      updateCheckboxes();
      updateMarkers();
    }
  })
}

function toggle(type, osm) {
  var checked = !climbed[osm];
  climbed[osm] = checked;
  localforage.setItem(CLIMBED_PEAKS, climbed);
  updateUi(type, osm);
}

function updateUi(type, osm) {
  adjustToggleLabel(osm);
  $("#" + osm).prop("checked", climbed[osm] || false);
  var icon = markers[osm].getIcon();
  icon.options.icon = climbed[osm] ? hasPhoto.has(osm) ? "photo" : "check" : "mountain";
  icon.options.markerColor = type == "百岳" ? climbed[osm] ? "blue" : "darkblue" : climbed[osm] ? "green" : "darkgreen";
  markers[osm].setIcon(icon);
  var toggleLabel = climbed[osm] ? "unvisited" : "visited";
  $("#toggleButton").text("Mark as " + toggleLabel);
  updateProgress();
}

// Update all checkbox values.
function updateCheckboxes() {
  for (osm of BAIYUE) {
    $("#" + osm).prop("checked", climbed[osm] || false);
  }
  for (osm of XIAOBAIYUE) {
    $("#" + osm).prop("checked", climbed[osm] || false);
  }
  for (osm of OLD_XIAOBAIYUE) {
    $("#" + osm).prop("checked", climbed[osm] || false);
  }
}

// Backup the climbing progress to a JSON file and download.
async function backupProgress() {
  var a = {};
  for (var i = 0; i < await localforage.length(); i++) {
    var k = await localforage.key(i);
    var v = await localforage.getItem(k);
    a[k] = v;
  }
  var textToSave = JSON.stringify(a)
  var textToSaveAsBlob = new Blob([textToSave], {
    type: "text/plain"
  });
  var textToSaveAsURL = window.URL.createObjectURL(textToSaveAsBlob);
  var downloadLink = document.createElement("a");
  downloadLink.download = "baiyue-backup.json";
  downloadLink.innerHTML = "Download File";
  downloadLink.href = textToSaveAsURL;
  downloadLink.onclick = function() {
    document.body.removeChild(event.target);
  };
  downloadLink.style.display = "none";
  document.body.appendChild(downloadLink);
  downloadLink.click();
}

// Restore the climbing progress from a JSON file.
function restoreProgress() {
  var input = document.createElement('input');
  input.type = 'file';
  input.onchange = e => {
    var file = e.target.files[0];
    var reader = new FileReader();
    reader.readAsText(file, 'UTF-8');
    reader.onload = readerEvent => {
      var content = readerEvent.target.result;
      try {
        t = JSON.parse(content);
        Promise.all(
          Object.keys(t).map(function(r) {
            return localforage.setItem(r, t[r]);
          })
        ).then(function() {
          localforage.getItem(CLIMBED_PEAKS).then(function(value) {
            climbed = value || {};
            updateProgress();
            updateCheckboxes();
            // clear markers
            for (marker of Object.values(markers)) {
              map.removeLayer(marker);
            };
            markers = [];
            initMarkers();
          });
        });
      } catch (a) {
        Swal.fire({
          title: "Error",
          text: "Loading data from the backup failed.",
          icon: "error",
          confirmButtonText: "Ok"
        });
        return;
      }
    }
  }
  input.click();
}


// Uploads a photo for a peak
function uploadPhoto(type, osm) {
  var input = document.createElement('input');
  input.type = 'file';
  input.onchange = e => {
    var file = e.target.files[0];
    var reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = readerEvent => {
      var content = readerEvent.target.result;
      const img = new Image();
      img.src = content;
      img.onload = function() {
        const maxWidth = 1600;
        const maxHeight = 1200;
        var ratio = Math.min(maxWidth / img.width, maxHeight / img.height);
        const canvas = document.createElement('canvas');
        canvas.width = img.width * ratio;
        canvas.height = img.height * ratio;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        const resizedBase64 = canvas.toDataURL('image/jpeg', 1.0);
        localforage.setItem("photo_" + osm, resizedBase64);
        hasPhoto.add(osm);
        updateUi(type, osm);
        localforage.setItem(HAS_PHOTO, hasPhoto);
        photoDialog(type, osm, resizedBase64, input);
      }
    }
  }
  localforage.getItem("photo_" + osm).then(function(value) {
    if (!value) {
      input.click();
    } else {
      photoDialog(type, osm, value, input);
    }
  })
}

function photoDialog(type, osm, value, input) {
  Swal.fire({
    imageUrl: value,
    focusConfirm: false,
    showDenyButton: true,
    denyButtonText: "Delete",
    confirmButtonText: "Update",
    cancelButtonText: "Close",
    showCancelButton: true,
    showCloseButton: true
  }).then((result) => {
    if (result.isDenied) {
      Swal.fire({
        title: "Are you sure?",
        text: "Deleting a photo cannot be undone.",
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#3085d6",
        cancelButtonColor: "#d33",
        confirmButtonText: "Yes"
      }).then((result) => {
        if (result.isConfirmed) {
          localforage.removeItem("photo_" + osm);
          hasPhoto.delete(osm);
          updateUi(type, osm);
          localforage.setItem(HAS_PHOTO, hasPhoto);
        }
        else {
          photoDialog(type, osm, value, input);
        }
      });
    }
    if (result.isConfirmed) {
      input.click();
    }
  })
}

function actionEvent(dropdown, value) {
  if (value.startsWith("http") || value.startsWith("om://")) {
    window.open(value);
  }
  else {
    navigator.clipboard.writeText(value);
  }
  // reset dropdown value
  dropdown.value = 0;
}

// Helper method for dropdown menu actions.
function menuEvent(value) {
  if (value == MENU_TAIWAN) {
    map.fitBounds(markerGroups["all"].getBounds(), {
      padding: [50, 50]
    });
  } else if (value == MENU_BACKUP) {
    backupProgress();
  } else if (value == MENU_RESTORE) {
    restoreProgress();
  } else if (value == MENU_RESET) {
    resetProgress();
  } else if (value == MENU_ABOUT) {
    history.pushState(null, null, "#");
    jumpTo("about");
  } else if (value == MENU_NORTH) {
    map.fitBounds(markerGroups["north"].getBounds(), {
      padding: [50, 50]
    });
  } else if (value == MENU_CENTRAL) {
    map.fitBounds(markerGroups["central"].getBounds(), {
      padding: [50, 50]
    });
  } else if (value == MENU_SOUTH) {
    map.fitBounds(markerGroups["south"].getBounds(), {
      padding: [50, 50]
    });
  } else if (value == MENU_EAST) {
    map.fitBounds(markerGroups["east"].getBounds(), {
      padding: [50, 50]
    });
  } else if (value == MENU_ISLANDS) {
    map.fitBounds(markerGroups["islands"].getBounds(), {
      padding: [50, 50]
    });
  } else if (value == MENU_LOCATION) {
    if (!currPos) {
      Swal.fire({
        text: "Your current positon is unknown.",
        icon: "warning"
      })
    }
    else {
      map.flyTo([currPos.coords.latitude, currPos.coords.longitude], FLY_TO_ZOOM);
    }
  }
  resetDropdown();
}
