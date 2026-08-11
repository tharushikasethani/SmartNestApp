
   import {
    ref,
    onValue,
    update
} from "https://www.gstatic.com/firebasejs/11.0.2/firebase-database.js";

(function () {
  "use strict";

  const homes = {};
  const floors = {};
  const devices = {};

  const USER_ID = "rlLqUU2AqcUjBjq03u4h8z8w1eK2";
  const DEFAULT_VIEWBOX = "0 0 860 570";
  const ROOM_PALETTE = ["#dbeafe", "#dcfce7", "#fef3c7", "#ffe4e6", "#ede9fe", "#cffafe"];
  const HOME_IMAGES = ["images/home001.jpg", "images/home002.jpg", "images/image3.jpg"];
  const FLOOR_IMAGES = ["images/floor1.jpg", "images/floor2.jpg", "images/floor2_2.jpg", "images/floor3.jpg"];
  let firebaseBasePath = USER_ID;


  const state = { screen: "homes", homeId: null, floorId: null };
  const app = document.getElementById("app");
  const breadcrumbEl = document.getElementById("breadcrumb");

  /* Theme Toggle Setup */
  const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
  const savedTheme = localStorage.getItem("smartnest-theme");
  const theme = savedTheme || (prefersDark ? "dark" : "light");
  if (theme === "dark") document.body.classList.add("theme-dark");
  localStorage.setItem("smartnest-theme", theme);

  /* Theme toggle button will be added dynamically in render() function */

  function toggleTheme() {
    const isDark = document.body.classList.toggle("theme-dark");
    localStorage.setItem("smartnest-theme", isDark ? "dark" : "light");
    const btn = document.getElementById("themeToggleBtn");
    if (btn) btn.innerHTML = isDark ? "☀️" : "🌙";
  }

  document.getElementById("brandHome").addEventListener("click", goHomes);
  document.getElementById("brandHome").addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") goHomes(); });

  function goHomes() { state.screen = "homes"; state.homeId = null; state.floorId = null; render(); }
  function goFloors(homeId) { state.screen = "floors"; state.homeId = homeId; state.floorId = null; render(); }
  function goFloorPlan(floorId) { state.screen = "floorplan"; state.floorId = floorId; render(); }

  /* ============================================================
     3. DATA HELPERS   ← FIREBASE swap points
     ============================================================ */

  function getDevice(id) { return devices[id]; }

  function devicesForFloor(floorId) {
    return Object.keys(devices)
      .filter(id => devices[id].floorId === floorId)
      .map(id => ({ id, ...devices[id] }));
  }

  function setDeviceState(id, patch) {
    const device = devices[id];
    if (!device) return;

    if (Object.prototype.hasOwnProperty.call(patch, "on")) {
      device.on = !!patch.on;
    }
    if (Object.prototype.hasOwnProperty.call(patch, "motion")) {
      device.motion = !!patch.motion;
    }
    if (Object.prototype.hasOwnProperty.call(patch, "value")) {
      device.value = patch.value;
    }

    updatePlanIconDot(id);

    const payload = {};
    if (Object.prototype.hasOwnProperty.call(patch, "on")) {
      payload.status = patch.on ? "ON" : "OFF";
    }
    if (Object.prototype.hasOwnProperty.call(patch, "motion")) {
      payload.motion = !!patch.motion;
    }
    if (Object.prototype.hasOwnProperty.call(patch, "value")) {
      payload.temperature = patch.value;
    }

    if (Object.keys(payload).length === 0) return;
    update(ref(window.db, device.firebasePath), payload);
  }

  function buildFirebasePath(...parts) {
    return parts.filter(Boolean).join("/");
  }

  function extractUserRecord(snapshotData) {
    if (!snapshotData) return null;
    if (snapshotData.homes) {
      return { record: snapshotData, basePath: "" };
    }
    if (snapshotData[USER_ID]?.homes) {
      return { record: snapshotData[USER_ID], basePath: USER_ID };
    }
    if (snapshotData.users?.[USER_ID]?.homes) {
      return { record: snapshotData.users[USER_ID], basePath: buildFirebasePath("users", USER_ID) };
    }
    return null;
  }

  function getHomeImage(index) {
    return HOME_IMAGES[index % HOME_IMAGES.length];
  }

  function getFloorImage(index) {
    return FLOOR_IMAGES[index % FLOOR_IMAGES.length];
  }

  function roomLayoutColor(index) {
    return ROOM_PALETTE[index % ROOM_PALETTE.length];
  }

  function hasPoint(value) {
    return value && Number.isFinite(value.x) && Number.isFinite(value.y);
  }

  function hasRect(value) {
    return value && Number.isFinite(value.x) && Number.isFinite(value.y) && Number.isFinite(value.w) && Number.isFinite(value.h);
  }

  function normalizeKey(value) {
    return String(value || "").toLowerCase().replace(/[^a-z0-9]+/g, "").trim();
  }

  function getRoomLayoutAlias(roomId, room) {
    const idKey = normalizeKey(roomId);
    const nameKey = normalizeKey(room?.name || room?.label || roomId);

    if (idKey.includes("kitchen") || nameKey.includes("kitchen")) return "kitchen";
    if (idKey.includes("bedroom") || nameKey.includes("bedroom")) return "bedroom";
    if (idKey.includes("bathroom") || nameKey.includes("bathroom")) return "bathroom";
    if (idKey.includes("living") || nameKey.includes("living")) return "living";
    if (idKey.includes("dining") || nameKey.includes("dining")) return "dining";
    if (idKey.includes("laundry") || nameKey.includes("laundry")) return "laundry";
    if (idKey.includes("porch") || nameKey.includes("porch")) return "porch";
    if (idKey.includes("deck") || nameKey.includes("deck")) return "deck";
    if (idKey.includes("media") || nameKey.includes("media")) return "media";
    return null;
  }

  /* ============================================================
   PREDEFINED FLOOR ROOM POSITIONS
   ============================================================*/

const PREDEFINED_FLOOR_LAYOUTS = {
  floor001: {
    viewBox: "0 0 860 570",
    rooms: [
      { id: "media",    label: "Media Room",   color: "#aecbf2", labelColor: "#2a4f8e", x: 10,  y: 10,  w: 200, h: 340 },
      { id: "deck",     label: "Deck",          color: "#cdc3ef", labelColor: "#5a4a9e", x: 210, y: 10,  w: 450, h: 100 },
      { id: "living",   label: "Living Room",   color: "#b7e6c0", labelColor: "#2e6b42", x: 210, y: 110, w: 450, h: 240 },
      { id: "kitchen",  label: "Kitchen",       color: "#f7dd9b", labelColor: "#8b6a1c", x: 660, y: 10,  w: 190, h: 340 },
      { id: "bedroom",  label: "Bedroom",       color: "#f3bcd4", labelColor: "#933c63", x: 10,  y: 350, w: 220, h: 210 },
      { id: "porch",    label: "Porch",         color: "#dcd9a6", labelColor: "#706b21", x: 230, y: 350, w: 130, h: 210 },
      { id: "dining",   label: "Dining Room",   color: "#a9d8ee", labelColor: "#1f5f82", x: 360, y: 350, w: 190, h: 210 },
      { id: "laundry",  label: "Laundry",       color: "#a8c8ef", labelColor: "#25477f", x: 550, y: 350, w: 130, h: 210 },
      { id: "bathroom", label: "Bathroom",      color: "#a7e2cf", labelColor: "#1c7560", x: 680, y: 350, w: 170, h: 210 }
    ]
  },

  floor002: {
    viewBox: "0 0 860 500",
    rooms: [
      { id: "bedroom2",  label: "Master Bedroom", color: "#f3bcd4", labelColor: "#933c63", x: 20,  y: 20,  w: 440, h: 320 },
      { id: "bathroom2", label: "Bathroom",       color: "#a7e2cf", labelColor: "#1c7560", x: 490, y: 20,  w: 350, h: 320 },
      { id: "landing",   label: "Landing",        color: "#dde8f5", labelColor: "#4b6b9e", x: 20,  y: 370, w: 820, h: 110 }
    ]
  },

  floor003: {
    viewBox: "0 0 800 480",
    rooms: [
      { id: "living2",  label: "Living Room", color: "#b7e6c0", labelColor: "#2e6b42", x: 30,  y: 30, w: 350, h: 420 },
      { id: "kitchen2", label: "Kitchen",     color: "#f7dd9b", labelColor: "#8b6a1c", x: 410, y: 30, w: 360, h: 420 }
    ]
  }
}; 

/* ============================================================
   PREDEFINED DEVICE POSITIONS
   ============================================================ */

const PREDEFINED_DEVICE_POSITIONS = {

  /* ----- Ground Floor ----- */

  tv001:    { x: 120, y: 150 },
  lamp001:  { x: 40,  y: 80 },

  camera001: { x: 370, y: 50 },

  light001:  { x: 390, y: 185 },
  fan001:    { x: 450, y: 185 },
  sensor001: { x: 460, y: 320 },

  fridge001: { x: 680, y: 60 },
  oven001:   { x: 810, y: 325 },
  light002:  { x: 750, y: 210 },
  plug001:   { x: 820, y: 130 },
  blinds001: { x: 825, y: 70 },

  light003:  { x: 135, y: 480 },
  blinds004: { x: 30,  y: 535 },
  plug002:   { x: 30,  y: 370 },

  door001:   { x: 310, y: 410 },
  speaker04: { x: 300, y: 370 },
  camera002: { x: 325, y: 535 },
  light007:  { x: 290, y: 535 },

  light004:  { x: 470, y: 410 },
  fan002:    { x: 430, y: 410 },

  washer001: { x: 615, y: 370 },
  plug003:   { x: 615, y: 520 },
  light005:  { x: 630, y: 425 },

  sensor002: { x: 800, y: 520 },
  light006:  { x: 750, y: 425 },


  /* ----- First Floor ----- */

  fan003:    { x: 240, y: 180 },
  sensor003: { x: 410, y: 100 },


  /* ----- Apartment ----- */

  lightA:    { x: 205, y: 240 },
  plugA:     { x: 590, y: 240 }
};

const PREDEFINED_ROOM_DEVICE_SLOTS = {
  media:    [{ x: 120, y: 150 }, { x: 40, y: 80 }],
  deck:     [{ x: 370, y: 50 }],
  living:   [{ x: 390, y: 185 }, { x: 450, y: 185 }, { x: 460, y: 320 }],
  kitchen:  [{ x: 680, y: 60 }, { x: 810, y: 325 }, { x: 750, y: 210 }, { x: 820, y: 130 }, { x: 825, y: 70 }],
  bedroom:  [{ x: 135, y: 480 }, { x: 30, y: 535 }, { x: 30, y: 370 }],
  porch:    [{ x: 310, y: 410 }, { x: 300, y: 370 }, { x: 325, y: 535 }, { x: 290, y: 535 }],
  dining:   [{ x: 470, y: 410 }, { x: 430, y: 410 }],
  laundry:  [{ x: 615, y: 370 }, { x: 615, y: 520 }, { x: 630, y: 425 }],
  bathroom: [{ x: 800, y: 520 }, { x: 750, y: 425 }],
  bedroom2: [{ x: 240, y: 180 }, { x: 410, y: 100 }],
  living2:  [{ x: 205, y: 240 }],
  kitchen2: [{ x: 590, y: 240 }]
};

function getRoomDeviceSlots(roomId, room, roomLayout) {
  const roomAlias = getRoomLayoutAlias(roomId, room) || normalizeKey(roomId);
  const slots = PREDEFINED_ROOM_DEVICE_SLOTS[roomAlias] || [];

  if (slots.length) {
    return slots.map(slot => ({ x: slot.x, y: slot.y }));
  }

  if (roomLayout && Number.isFinite(roomLayout.x) && Number.isFinite(roomLayout.y)) {
    return [
      { x: roomLayout.x + 40, y: roomLayout.y + 50 },
      { x: roomLayout.x + roomLayout.w / 2, y: roomLayout.y + roomLayout.h / 2 },
      { x: roomLayout.x + roomLayout.w - 50, y: roomLayout.y + 70 }
    ];
  }

  return [];
}

function getPredefinedFloorLayout(floorId, floorIndex = 0, floorName = "") {
  if (PREDEFINED_FLOOR_LAYOUTS[floorId]) {
    return PREDEFINED_FLOOR_LAYOUTS[floorId];
  }

  const idKey = normalizeKey(floorId);
  const nameKey = normalizeKey(floorName);

  for (const [key, layout] of Object.entries(PREDEFINED_FLOOR_LAYOUTS)) {
    const normKey = normalizeKey(key);
    if (idKey === normKey || idKey.includes(normKey) || normKey.includes(idKey)) {
      return layout;
    }
  }

  if (idKey.includes("ground") || nameKey.includes("ground") || idKey.includes("1") || nameKey.includes("1") || nameKey.includes("first")) {
    return PREDEFINED_FLOOR_LAYOUTS.floor001;
  }
  if (idKey.includes("2") || nameKey.includes("2") || nameKey.includes("second") || nameKey.includes("master")) {
    return PREDEFINED_FLOOR_LAYOUTS.floor002;
  }
  if (idKey.includes("3") || nameKey.includes("3") || nameKey.includes("third") || nameKey.includes("apartment")) {
    return PREDEFINED_FLOOR_LAYOUTS.floor003;
  }

  const keys = Object.keys(PREDEFINED_FLOOR_LAYOUTS);
  return PREDEFINED_FLOOR_LAYOUTS[keys[floorIndex % keys.length]] || PREDEFINED_FLOOR_LAYOUTS.floor001;
}

function layoutRooms(roomEntries, floorId, floorIndex = 0, floorName = "") {

  const predefined = getPredefinedFloorLayout(floorId, floorIndex, floorName);
  const autoLayouts = layoutRoomsAutomatically(roomEntries);
  const usedPredefinedIndices = new Set();

  return roomEntries.map(([roomId, room], index) => {
    const roomAlias = getRoomLayoutAlias(roomId, room);
    const roomNameNorm = normalizeKey(room?.name || room?.label || roomId);

    let predefinedIndex = -1;
    if (predefined?.rooms) {
      predefinedIndex = predefined.rooms.findIndex((r, idx) => 
        !usedPredefinedIndices.has(idx) && (
          r.id === roomId || 
          r.id === roomAlias || 
          normalizeKey(r.label) === roomNameNorm ||
          roomNameNorm.includes(r.id) ||
          r.id.includes(roomNameNorm)
        )
      );

      if (predefinedIndex === -1) {
        predefinedIndex = predefined.rooms.findIndex((_, idx) => !usedPredefinedIndices.has(idx));
      }
    }

    let predefinedRoom = null;
    if (predefinedIndex !== -1) {
      usedPredefinedIndices.add(predefinedIndex);
      predefinedRoom = predefined.rooms[predefinedIndex];
    }

    const explicitRoom = hasRect(room) ? room : null;
    const layout = explicitRoom || predefinedRoom || autoLayouts[index];

    return {
      id: roomId,
      label: room.name || predefinedRoom?.label || layout.label || "Room",
      icon: room.icon || predefinedRoom?.icon || layout.icon || "room",
      color: room.color || predefinedRoom?.color || layout.color || roomLayoutColor(index),
      labelColor: room.labelColor || predefinedRoom?.labelColor || layout.labelColor || "#000",
      x: layout.x,
      y: layout.y,
      w: layout.w,
      h: layout.h
    };
  });
}
  
function layoutRoomsAutomatically(roomEntries) {

  const count = roomEntries.length;
  if (!count) return [];

  const viewBoxWidth = 860;
  const viewBoxHeight = 570;

  const cols = Math.ceil(Math.sqrt(count));
  const rows = Math.ceil(count / cols);

  const paddingX = 48;
  const paddingY = 48;
  const gapX = 22;
  const gapY = 22;

  const roomWidth = Math.floor(
    (viewBoxWidth - paddingX * 2 - gapX * (cols - 1)) / cols
  );

  const roomHeight = Math.floor(
    (viewBoxHeight - paddingY * 2 - gapY * (rows - 1)) / rows
  );

  return roomEntries.map(([roomId, room], index) => {

    const column = index % cols;
    const row = Math.floor(index / cols);

    return {
      id: roomId,
      label: room.name || "Room",
      icon: room.icon || "room",
      color: roomLayoutColor(index),
      labelColor: "#000",

      x: Math.round(
        paddingX + column * (roomWidth + gapX)
      ),

      y: Math.round(
        paddingY + row * (roomHeight + gapY)
      ),

      w: roomWidth,
      h: roomHeight
    };
  });
}

function devicePositionsInRoom(room, deviceEntries) {

  if (!room || !deviceEntries.length) return [];

  const roomId = room.id || room.roomId || room.name || "";
  const roomSlots = getRoomDeviceSlots(roomId, room, room);

  return deviceEntries.map(([deviceId, device], index) => {

    if (hasPoint(device?.pos)) {
      return {
        x: device.pos.x,
        y: device.pos.y
      };
    }

    if (Number.isFinite(device?.x) && Number.isFinite(device?.y)) {
      return {
        x: device.x,
        y: device.y
      };
    }

    /*
     * Use predefined position if available.
     */
    if (PREDEFINED_DEVICE_POSITIONS[deviceId]) {
      return PREDEFINED_DEVICE_POSITIONS[deviceId];
    }

    if (roomSlots[index]) {
      return roomSlots[index];
    }

    /*
     * Otherwise use automatic positioning.
     */
    const deviceCount = deviceEntries.length;

    const columns = Math.min(3, deviceCount);
    const rows = Math.ceil(deviceCount / columns);

    const innerWidth = Math.max(1, room.w - 72);
    const innerHeight = Math.max(1, room.h - 72);

    const stepX = innerWidth / columns;
    const stepY = innerHeight / rows;

    const column = index % columns;
    const row = Math.floor(index / columns);

    return {
      x: Math.round(
        room.x + 36 + stepX / 2 + column * stepX
      ),

      y: Math.round(
        room.y + 36 + stepY / 2 + row * stepY
      )
    };
  });
}

  /* ============================================================
     4. BREADCRUMB
     ============================================================ */

  function renderBreadcrumb() {
    const parts = [{ label: "Homes", onClick: goHomes }];
    if (state.homeId) { const h = homes[state.homeId]; parts.push({ label: h.name, onClick: () => goFloors(state.homeId) }); }
    if (state.floorId) { parts.push({ label: floors[state.floorId].name, onClick: null }); }

    breadcrumbEl.innerHTML = "";
    parts.forEach((p, i) => {
      if (i > 0) { const sep = document.createElement("span"); sep.className = "sep"; sep.textContent = "›"; breadcrumbEl.appendChild(sep); }
      const c = document.createElement("span");
      c.className = "crumb" + (p.onClick ? "" : " current");
      c.textContent = p.label;
      if (p.onClick) c.addEventListener("click", p.onClick);
      breadcrumbEl.appendChild(c);
    });
  }

  /* ============================================================
     5. SCREEN — HOMES
     ============================================================ */

  function renderHomesScreen() {
    app.innerHTML = `
    <section class="screen">
      <div class="screen-head">
        <h2>Select a home</h2>
        <p>Choose which property you would like to monitor and control.</p>
      </div>
      <div class="home-grid">
        ${Object.values(homes).map(home => {
      const fc = home.floorIds.length;
      const dc = Object.values(devices).filter(d => d.homeId === home.id).length;
      return `<article class="home-card" data-home="${home.id}" tabindex="0"
                    style="background-image:url('${home.image}');--home-tone-a:${home.tone[0]};--home-tone-b:${home.tone[1]}">
            <div class="home-card-overlay"></div>
            <div class="home-card-arrow">›</div>
            <div class="home-card-body">
              <h3>${home.name}</h3>
              <p>${home.address}</p>
              <div class="home-card-meta">
                <span class="meta-chip">${fc} floor${fc > 1 ? "s" : ""}</span>
                <span class="meta-chip">${dc} devices</span>
              </div>
            </div>
          </article>`;
    }).join("")}
      </div>
    </section>`;

    app.querySelectorAll(".home-card").forEach(card => {
      const open = () => goFloors(card.dataset.home);
      card.addEventListener("click", open);
      card.addEventListener("keydown", e => { if (e.key === "Enter" || e.key === " ") open(); });
    });
  }

  function renderStatusScreen(title, message, hint = "") {
    app.innerHTML = `
    <section class="screen">
      <div class="screen-head">
        <h2>${title}</h2>
        <p>${message}</p>
      </div>
      ${hint ? `<div class="plan-panel" style="padding:18px 20px;color:var(--ink-soft);font-size:13px;line-height:1.6">${hint}</div>` : ""}
    </section>`;
  }

  /* ============================================================
     6. SCREEN — FLOORS
     ============================================================ */

  function renderFloorsScreen() {
    const home = homes[state.homeId];
    app.innerHTML = `
    <section class="screen">
      <button class="back-btn" id="backToHomes">← All homes</button>
      <div class="screen-head">
        <h2>${home.icon} ${home.name}</h2>
        <p>Pick a floor to open its interactive floor plan.</p>
      </div>
      <div class="floor-grid">
        ${home.floorIds.map(fid => {
      const fl = floors[fid];
      const fdevs = devicesForFloor(fid);
      const on = fdevs.filter(d => d.on || d.motion).length;
      return `<article class="floor-card" data-floor="${fid}" tabindex="0">
            <div class="floor-card-image">
        <img src="${fl.image}" alt="${fl.name}">
    </div>
            <h3>${fl.name}</h3>
            <p>${fl.rooms.length} rooms · ${fdevs.length} devices</p>
            <div class="floor-card-meta"><span class="meta-chip">${on} active</span></div>
          </article>`;
    }).join("")}
      </div>
    </section>`;

    document.getElementById("backToHomes").addEventListener("click", goHomes);
    app.querySelectorAll(".floor-card").forEach(card => {
      const open = () => goFloorPlan(card.dataset.floor);
      card.addEventListener("click", open);
      card.addEventListener("keydown", e => { if (e.key === "Enter" || e.key === " ") open(); });
    });
  }

  /* ============================================================
     7. SCREEN — FLOOR PLAN  (full-width, no device list)
     ============================================================ */

  function renderFloorPlanScreen() {
    const floor = floors[state.floorId];
    const home = homes[floor.homeId];
    const fdevs = devicesForFloor(floor.id);
    const [, , vbW, vbH] = floor.viewBox.split(" ").map(Number);

    /* SVG room shapes */
    const roomSVG = floor.rooms.map(r => {
      const mx = r.x + r.w / 2, my = r.y + r.h / 2 + 6;
      return `<rect class="room-shape" x="${r.x}" y="${r.y}" width="${r.w}" height="${r.h}" rx="4" fill="${r.color}"/>
              <text class="room-label" 
              x="${mx}" 
              y="${my}" 
              text-anchor="middle">
              ${r.label}
              </text>`;
    }).join("");

    /* Icon overlay badges */
    const iconHTML = fdevs.map(d => {
      const lp = (d.pos.x / vbW * 100).toFixed(2);
      const tp = (d.pos.y / vbH * 100).toFixed(2);
      let dotCls = "off";
      let tag = "";
      if (d.type === "switch" || d.type === "lock") dotCls = d.on ? "on" : "off";
      else if (d.type === "temp") { dotCls = "sensor"; tag = `${d.value}°C`; }
      else if (d.type === "motion") dotCls = d.motion ? "motion-active" : "sensor";

      return `<button class="plan-icon ${dotCls}" 
        style="left:${lp}%;top:${tp}%;" 
        data-plan-btn="${d.id}" 
        data-tooltip="${d.name} : ${d.type === "switch" ? (d.on ? "ON" : "OFF") :
          d.type === "temp" ? d.value + "°C" :
            d.motion ? "MOTION DETECTED" : "NO MOTION"
        }"
        title="${d.name}" 
        type="button">

        <span class="plan-icon-glyph">${d.image ? `<img src="${d.image}" class="device-image" alt="${d.name}" onerror="this.onerror=null;this.src='images/devices/smart_plug.png';">` : d.icon}</span>

        <span class="plan-icon-dot ${dotCls}" data-dot="${d.id}"></span>

        ${tag ? `<span class="plan-icon-tag">${tag}</span>` : ""}

        </button>`;
    }).join("");

    app.innerHTML = `
    <section class="screen">
      <button class="back-btn" id="backToFloors">← ${home.name} floors</button>
      <div class="screen-head">
        <h2>${floor.name}</h2>
        <p>Hover over any device to view status. Click to open controls.</p>
      </div>
      <div class="plan-panel">
        <div class="plan-panel-head">
          <h3>Floor Plan — ${floor.name}</h3>
          <div class="plan-legend">
            <span><span class="legend-dot on"></span>Active</span>
            <span><span class="legend-dot off"></span>Idle</span>
            <span><span class="legend-dot sensor"></span>Sensor</span>
          </div>
        </div>
        <div class="plan-wrap">
          <svg viewBox="${floor.viewBox}" preserveAspectRatio="xMidYMid meet">${roomSVG}</svg>
          ${iconHTML}
        </div>
      </div>
    </section>`;

    document.getElementById("backToFloors").addEventListener("click", () => goFloors(home.id));

    /* Each plan icon click opens the modal */
    app.querySelectorAll("[data-plan-btn]").forEach(btn => {
      btn.addEventListener("click", () => openDeviceModal(btn.dataset.planBtn));
    });

    renderBreadcrumb();
  }

  /* ============================================================
     8. DEVICE MODAL
     ============================================================ */

  function openDeviceModal(id) {
    closeDeviceModal(true);
    const d = getDevice(id);
    if (!d) return;
    const floor = floors[d.floorId];
    const roomLabel = floor.rooms.find(r => r.id === d.room)?.label || d.room;

    let bodyHTML = "";

    if (d.type === "switch" || d.type === "lock") {
      const isLock = d.type === "lock";
      const statusLabel = isLock ? (d.on ? "OPEN" : "LOCKED") : (d.on ? "ON" : "OFF");
      const badgeCls = d.on ? "badge-on" : "badge-off";
      const detail = d.watts ? `<span style="font-size:13px;color:var(--ink-soft);margin-left:8px;">${d.watts}W</span>` : "";
      bodyHTML = `
        <div class="modal-status-row">
          <div class="modal-status-left">
            <span class="modal-status-key">Status</span>
            <span class="modal-status-val"><span class="badge ${badgeCls}">${statusLabel}</span>${detail}</span>
          </div>
          <div class="modal-toggle-row" style="margin:0">
            <label class="switch">
              <input type="checkbox" id="modalToggle" ${d.on ? "checked" : ""} />
              <span class="slider"></span>
            </label>
          </div>
        </div>`;
    } else if (d.type === "temp") {
      bodyHTML = `
        <div class="modal-status-row" style="flex-direction:column;align-items:center;gap:10px">
          <span class="modal-status-key">Current Reading</span>
          <div class="modal-reading">${d.value}<small>°C</small></div>
        </div>
        <button class="modal-action" id="modalSimTemp">🔄 Simulate New Reading</button>`;
    } else if (d.type === "motion") {
      const detected = d.motion;
      bodyHTML = `
        <div class="modal-status-row" style="flex-direction:column;align-items:center;gap:12px">
          <span class="modal-status-key">Motion Status</span>
          <span class="badge ${detected ? "badge-warn" : "badge-off"}" style="font-size:14px;padding:10px 22px;">
            ${detected ? "⚠️  MOTION DETECTED" : "NO MOTION"}
          </span>
        </div>
        ${!detected ? `<button class="modal-action" id="modalSimMotion">🚨 Trigger Motion Event</button>`
          : `<p style="text-align:center;color:var(--ink-soft);font-size:13px;margin:10px 0 0">Clears automatically in 4 s</p>`}`;
    }

    const backdrop = document.createElement("div");
    backdrop.id = "deviceModal";
    backdrop.className = "modal-backdrop";
    backdrop.dataset.deviceId = id;
    backdrop.innerHTML = `
      <div class="modal-card" role="dialog" aria-modal="true" aria-label="${d.name}">
        <button class="modal-close" id="modalClose" title="Close">✕</button>
        <div class="modal-icon-wrap">${d.image ? `<img src="${d.image}" class="device-image modal-device-image" alt="${d.name}" style="width:48px;height:48px;object-fit:contain;" onerror="this.onerror=null;this.src='images/devices/smart_plug.png';">` : d.icon}</div>
        <h2 class="modal-name">${d.name}</h2>
        <p class="modal-room">${roomLabel} · ${floor.name}</p>
        ${bodyHTML}
      </div>`;

    document.body.appendChild(backdrop);

    /* Wire up controls */
    document.getElementById("modalClose").addEventListener("click", () => closeDeviceModal());
    backdrop.addEventListener("click", e => { if (e.target === backdrop) closeDeviceModal(); });

    const toggle = document.getElementById("modalToggle");
    if (toggle) {
      toggle.addEventListener("change", () => setDeviceState(id, { on: toggle.checked }));
    }
    const simTemp = document.getElementById("modalSimTemp");
    if (simTemp) {
      simTemp.addEventListener("click", () => {
        setDeviceState(id, { value: randomBetween(18, 34) });
      });
    }
    const simMotion = document.getElementById("modalSimMotion");
    if (simMotion) {
      simMotion.addEventListener("click", () => {
        setDeviceState(id, { motion: true });
        setTimeout(() => setDeviceState(id, { motion: false }), 4000);
      });
    }

    /* Trap focus */
    backdrop.querySelector(".modal-card").focus?.();
    document.addEventListener("keydown", onModalKeydown);
  }

  function closeDeviceModal(silent = false) {
    const m = document.getElementById("deviceModal");
    if (m) { document.body.removeChild(m); }
    document.removeEventListener("keydown", onModalKeydown);
  }

  function onModalKeydown(e) {
    if (e.key === "Escape") closeDeviceModal();
  }

  /* Update only the status dot on the plan without a full re-render */
  function updatePlanIconDot(id) {
    const btn = app.querySelector(`[data-plan-btn="${id}"]`);
    const dot = app.querySelector(`[data-dot="${id}"]`);
    const d = getDevice(id);
    if (!d) return;

    let cls = "off";
    if (d.type === "switch" || d.type === "lock") cls = d.on ? "on" : "off";
    else if (d.type === "temp") cls = "sensor";
    else if (d.type === "motion") cls = d.motion ? "motion-active" : "sensor";

    if (btn) {
      btn.classList.remove("on", "off", "sensor", "motion-active");
      btn.classList.add(cls);
    }
    if (dot) {
      dot.className = "plan-icon-dot " + cls;
    }

    /* Update temp tag text if present */
    if (d.type === "temp" && btn) {
      const tag = btn.querySelector(".plan-icon-tag");
      if (tag) tag.textContent = `${d.value}°C`;
    }
  }

  /* ============================================================
     9. ROUTER
     ============================================================ */

  const origRender = function render() {
    renderBreadcrumb();
    if (state.screen === "homes") renderHomesScreen();
    else if (state.screen === "floors") renderFloorsScreen();
    else if (state.screen === "floorplan") renderFloorPlanScreen();
    window.scrollTo({ top: 0, behavior: "smooth" });

    /* Add theme toggle button if not already present */
    const topbarInner = document.querySelector(".topbar-inner");
    if (topbarInner && !document.getElementById("themeToggleBtn")) {
      const btn = document.createElement("button");
      btn.id = "themeToggleBtn";
      btn.className = "theme-toggle";
      btn.title = "Toggle light/dark theme";
      btn.innerHTML = document.body.classList.contains("theme-dark") ? "☀️" : "🌙";
      btn.addEventListener("click", toggleTheme);
      topbarInner.appendChild(btn);
    }
  };

  function render() { return origRender.call(this); }

  /* ============================================================
     10. UTILITIES
     ============================================================ */

  function randomBetween(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }

  (function linkPulse() {
    const el = document.getElementById("linkText");
    if (!el) return;
    const msgs = ["firebase · live sync", "syncing device tree", "firebase · live sync", "heartbeat OK"];
    let i = 0;
    setInterval(() => { i = (i + 1) % msgs.length; el.textContent = msgs[i]; }, 3500);
  })();

  function loadFirebaseData() {
    if (!window.db) {
      renderStatusScreen(
        "Firebase not ready",
        "The database connection has not been initialized yet.",
        "Check that firebase.js is loading before script.js and that the browser console shows the Firebase connection message."
      );
      return;
    }

    renderStatusScreen(
      "Loading data",
      "Connecting to your Firebase Realtime Database...",
      "If this stays here, the selected database path does not contain the expected user tree or the read is blocked by rules."
    );

    const dbRef = ref(window.db);

    onValue(dbRef, (snapshot) => {
      const extracted = extractUserRecord(snapshot.val());
      if (!extracted) {
        renderStatusScreen(
          "No SmartNest data found",
          "Firebase connected, but the app could not find the expected user record.",
          `Expected either a root user node named <strong>${USER_ID}</strong> or <strong>users/${USER_ID}</strong> with nested homes/floors/rooms/devices.`
        );
        return;
      }
      firebaseBasePath = extracted.basePath;
      convertFirebaseData(extracted.record);
    });
  }

  function convertFirebaseData(data) {
    const openModalId = document.getElementById("deviceModal")?.dataset.deviceId || null;

    Object.keys(homes).forEach(k => delete homes[k]);
    Object.keys(floors).forEach(k => delete floors[k]);
    Object.keys(devices).forEach(k => delete devices[k]);

    const homeEntries = Object.entries(data.homes || {});
    if (!homeEntries.length) {
      render();
      return;
    }

    homeEntries.forEach(([homeId, home], homeIndex) => {
      const floorEntries = Object.entries(home.floors || {});

      homes[homeId] = {
        id: homeId,
        name: home.name || "Home",
        address: home.address || "No address set",
        icon: home.type === "house" ? "🏠" : "🏡",
        image: home.image || getHomeImage(homeIndex),
        tone: home.tone || ["#7c3aed", "#f97316"],
        floorIds: []
      };

      floorEntries.forEach(([floorId, floor], floorIndex) => {
        const roomEntries = Object.entries(floor.rooms || {});
        const roomLayouts = layoutRooms(roomEntries, floorId, floorIndex, floor.name);

        homes[homeId].floorIds.push(floorId);

        floors[floorId] = {
          id: floorId,
          homeId,
          name: floor.name || "Floor",
          image: floor.image || getFloorImage(floorIndex),
          viewBox: DEFAULT_VIEWBOX,
          rooms: roomLayouts
        };

        roomEntries.forEach(([roomId, room], roomIndex) => {
          const roomLayout = roomLayouts[roomIndex] || roomLayouts[0];
          const deviceEntries = Object.entries(room.devices || {});
          const positions = devicePositionsInRoom(roomLayout, deviceEntries);
                    deviceEntries.forEach(([deviceId, device], deviceIndex) => {
            const type = convertType(device.deviceType);
            const fallbackValue = Number.isFinite(device.temperature)
              ? device.temperature
              : Number.isFinite(device.value)
                ? device.value
                : randomBetween(18, 28);

            devices[deviceId] = {
              id: deviceId,
              homeId,
              floorId,
              room: roomId,
              firebasePath: buildFirebasePath(firebaseBasePath, "homes", homeId, "floors", floorId, "rooms", roomId, "devices", deviceId),
              name: device.deviceName || "Device",
              type,
              on: device.status === "ON",
              motion: !!device.motion,
              value: type === "temp" ? fallbackValue : undefined,
              icon: getIcon(device.deviceType),
              image: getDeviceImage(device.deviceType, device.deviceName, deviceId),
              pos: positions[deviceIndex] || (hasPoint(device?.pos) ? device.pos : { x: 120 + deviceIndex * 72, y: 120 + roomIndex * 72 })
            };
          });
        });
      });
    });

    if (state.screen === "floorplan" && state.floorId && !floors[state.floorId]) {
      goHomes();
      return;
    }

    render();

    if (openModalId && devices[openModalId]) {
      openDeviceModal(openModalId);
    }
  }
function convertType(type){

    switch(type){

        case "temperature_sensor":
            return "temp";

        case "motion_sensor":
            return "motion";

        default:
            return "switch";

    }

}
function getIcon(type) {
  const t = String(type || "").toLowerCase().trim();
  switch (t) {
    case "light": case "lamp": case "bulb":
      return "💡";
    case "speaker": case "audio":
      return "🔊";
    case "tv": case "television":
      return "📺";
    case "deck_camera": case "camera": case "motion_sensor": case "motion":
      return "📷";
    case "ceiling_fan": case "fan":
      return "🌀";
    case "temperature_sensor": case "temp":
      return "🌡️";
    case "heater":
      return "🔥";
    case "refrigerator": case "fridge":
      return "🧊";
    case "smart_plug": case "plug": case "socket":
      return "🔌";
    case "kitchen_oven": case "oven": case "stove":
      return "🍳";
    case "blinds": case "blind":
      return "🪟";
    case "iron":
      return "🔌";
    case "lock": case "door_lock": case "door":
      return "🔒";
    case "ac": case "air_conditioner":
      return "❄️";
    case "washing_machine": case "washer":
      return "🧺";
    default:
      return "⚙️";
  }
}

function getDeviceImage(type, deviceName = "", deviceId = "") {
  const t = String(type || "").toLowerCase().trim();
  const n = String(deviceName || "").toLowerCase().trim();
  const id = String(deviceId || "").toLowerCase().trim();

  // Ceiling Fan
  if (t === "ceiling_fan" || t === "fan" || n.includes("fan") || id.includes("fan")) {
    return "images/devices/ceiling_fan.png";
  }
  // Camera / Motion Sensor
  if (t === "deck_camera" || t === "camera" || t === "motion_sensor" || t === "motion" || n.includes("camera") || id.includes("camera") || id.includes("sensor")) {
    return "images/devices/camera.png";
  }
  // Refrigerator
  if (t === "refrigerator" || t === "fridge" || n.includes("fridge") || n.includes("refrigerator") || id.includes("fridge")) {
    return "images/devices/refrigerator.png";
  }
  // Washing Machine
  if (t === "washing_machine" || t === "washer" || n.includes("washer") || n.includes("laundry") || n.includes("wash") || id.includes("washer")) {
    return "images/devices/washing_machine.png";
  }
  // AC / Air Conditioner
  if (t === "ac" || t === "air_conditioner" || n.includes("ac") || n.includes("air") || n.includes("cooler") || id.includes("ac")) {
    return "images/devices/AC.png";
  }
  // Oven
  if (t === "kitchen_oven" || t === "oven" || n.includes("oven") || id.includes("oven") || n.includes("stove") || n.includes("microwave")) {
    return "images/devices/oven.png";
  }
  // Heater / Temp Sensor
  if (t === "heater" || t === "temperature_sensor" || t === "temp" || n.includes("heater") || id.includes("heater") || n.includes("temp")) {
    return "images/devices/heater.png";
  }
  // Iron
  if (t === "iron" || n.includes("iron") || id.includes("iron")) {
    return "images/devices/iron.png";
  }
  // Light / Lamp / Bulb
  if (t === "light" || t === "lamp" || t === "bulb" || n.includes("light") || n.includes("lamp") || n.includes("bulb") || id.includes("light") || id.includes("lamp")) {
    return "images/devices/light.png";
  }
  // Lock / Door Lock
  if (t === "lock" || t === "door" || t === "door_lock" || n.includes("lock") || n.includes("door") || id.includes("lock")) {
    return "images/devices/lock.png";
  }
  // Blinds / Window
  if (t === "blinds" || t === "blind" || n.includes("blind") || n.includes("window") || id.includes("blind")) {
    return "images/devices/blind.png";
  }
  // TV / Television
  if (t === "tv" || t === "television" || n.includes("tv") || n.includes("television") || id.includes("tv")) {
    return "images/devices/tv.png";
  }
  // Speaker / Audio
  if (t === "speaker" || t === "audio" || n.includes("speaker") || n.includes("audio") || n.includes("sound") || id.includes("speaker")) {
    return "images/devices/speaker.png";
  }
  // Smart Plug
  if (t === "smart_plug" || t === "plug" || t === "socket" || n.includes("plug") || n.includes("socket") || id.includes("plug")) {
    return "images/devices/smart_plug.png";
  }

  return "images/devices/smart_plug.png";
}
  /* ============================================================
     BOOT
     ============================================================ */

loadFirebaseData();

})();

