
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

  function layoutRooms(roomEntries) {
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
    const roomWidth = Math.floor((viewBoxWidth - paddingX * 2 - gapX * (cols - 1)) / cols);
    const roomHeight = Math.floor((viewBoxHeight - paddingY * 2 - gapY * (rows - 1)) / rows);

    return roomEntries.map(([roomId, room], index) => {
      const column = index % cols;
      const row = Math.floor(index / cols);
      return {
        id: roomId,
        label: room.name || "Room",
        icon: room.icon || "room",
        color: roomLayoutColor(index),
        labelColor: "#000",
        x: Math.round(paddingX + column * (roomWidth + gapX)),
        y: Math.round(paddingY + row * (roomHeight + gapY)),
        w: roomWidth,
        h: roomHeight
      };
    });
  }

  function devicePositionsInRoom(room, deviceCount) {
    if (!room || !deviceCount) return [];

    const columns = Math.min(3, deviceCount);
    const rows = Math.ceil(deviceCount / columns);
    const innerWidth = Math.max(1, room.w - 72);
    const innerHeight = Math.max(1, room.h - 72);
    const stepX = innerWidth / columns;
    const stepY = innerHeight / rows;

    return Array.from({ length: deviceCount }, (_, index) => {
      const column = index % columns;
      const row = Math.floor(index / columns);
      return {
        x: Math.round(room.x + 36 + stepX / 2 + column * stepX),
        y: Math.round(room.y + 36 + stepY / 2 + row * stepY)
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

      return `<button class="plan-icon" 
        style="left:${lp}%;top:${tp}%;" 
        data-plan-btn="${d.id}" 
        data-tooltip="${d.name} : ${d.type === "switch" ? (d.on ? "ON" : "OFF") :
          d.type === "temp" ? d.value + "°C" :
            d.motion ? "MOTION DETECTED" : "NO MOTION"
        }"
        title="${d.name}" 
        type="button">

        <span class="plan-icon-glyph">${d.image ? `<img src="${d.image}" class="device-image">` : d.icon}</span>

        <span class="plan-icon-dot ${dotCls}" data-dot="${d.id}"></span>

        ${tag ? `<span class="plan-icon-tag">${tag}</span>` : ""}

        </button>`;
      return `<button class="plan-icon ${dotCls}" 
        style="left:${lp}%;top:${tp}%;"
        data-plan-btn="${d.id}" 
        title="${d.name}"
        type="button">

        <span class="device-tooltip">
          <strong>${d.name}</strong><br>
          ${d.type === "switch" || d.type === "lock"
          ? (d.on ? "🟢 ON" : "⚪ OFF")
          : d.type === "temp"
            ? `🌡️ ${d.value}°C`
            : d.motion
              ? "🚨 Motion Detected"
              : "No Motion"
        }
        </span>

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
        <div class="modal-icon-wrap">${d.icon}</div>
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
    const dot = app.querySelector(`[data-dot="${id}"]`);
    if (!dot) return;
    const d = getDevice(id);
    dot.className = "plan-icon-dot";
    if (d.type === "switch" || d.type === "lock") dot.classList.add(d.on ? "on" : "off");
    else if (d.type === "temp") dot.classList.add("sensor");
    else if (d.type === "motion") dot.classList.add(d.motion ? "motion-active" : "sensor");

    /* Update temp tag text if present */
    if (d.type === "temp") {
      const btn = app.querySelector(`[data-plan-btn="${id}"]`);
      const tag = btn?.querySelector(".plan-icon-tag");
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
        const roomLayouts = layoutRooms(roomEntries);

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
          const positions = devicePositionsInRoom(roomLayout, deviceEntries.length);

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
              pos: positions[deviceIndex] || { x: 120 + deviceIndex * 72, y: 120 + roomIndex * 72 }
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

    switch(type){

        case "light":
            return "💡";

        case "speaker":
            return "🔊";

        case "tv":
            return "📺";

        case "deck_camera":
            return "📷";

        case "ceiling_fan":
            return "🌀";

        case "temperature_sensor":
            return "🌡️";

        case "refrigerator":
            return "🧊";

        case "smart_plug":
            return "🔌";

        case "kitchen_oven":
            return "🍳";

        case "blinds":
            return "🪟";

        default:
            return "⚙️";

    }

}
  /* ============================================================
     BOOT
     ============================================================ */

loadFirebaseData();

})();

