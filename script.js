/* =========================================================
   SmartNest — Hardware Simulator  ·  script.js

   NAVIGATION:  Homes → Floors → Floor Plan (full-width)
   INTERACTION: tap a device icon on the plan → modal popup

   DATA mirrors Firebase Realtime Database:
     homes/  floors/  devices/

   Firebase swap points marked with  ← FIREBASE
   ========================================================= */

(function () {
  "use strict";

  /* ============================================================
     1. DATA
     ============================================================ */

  const homes = {
    home001: {
      id: "home001",
      name: "Green Valley Residence",
      address: "No. 24, Park Road, Colombo",
      icon: "🏡",
      image: "images/home001.jpg",
      tone: ["#1a2f5e", "#0d1b35"],
      floorIds: ["floor001", "floor002"],
    },
    home002: {
      id: "home002",
      name: "Lake View Apartment",
      address: "Unit 12B, Marine Drive",
      icon: "🏢",
      image: "images/home002.jpg",
      tone: ["#0d3322", "#061a14"],
      floorIds: ["floor003"],
    },
  };

  const floors = {
    floor001: {
      id: "floor001", homeId: "home001", name: "Ground Floor", image: "images/floor1.jpg",
      viewBox: "0 0 860 570",
      rooms: [
        { id: "media", label: "Media Room", color: "#aecbf2", labelColor: "#2a4f8e", x: 10, y: 10, w: 200, h: 340 },
        { id: "deck", label: "Deck", color: "#cdc3ef", labelColor: "#5a4a9e", x: 210, y: 10, w: 450, h: 100 },
        { id: "living", label: "Living Room", color: "#b7e6c0", labelColor: "#2e6b42", x: 210, y: 110, w: 450, h: 240 },
        { id: "kitchen", label: "Kitchen", color: "#f7dd9b", labelColor: "#8b6a1c", x: 660, y: 10, w: 190, h: 340 },
        // { id: "suite",   label: "Primary Suite", color: "#a6ded2", labelColor: "#1c7262", x: 660, y: 10,  w: 190, h: 340 },
        { id: "bedroom", label: "Bedroom", color: "#f3bcd4", labelColor: "#933c63", x: 10, y: 350, w: 220, h: 210 },
        { id: "porch", label: "Porch", color: "#dcd9a6", labelColor: "#706b21", x: 230, y: 350, w: 130, h: 210 },
        { id: "dining", label: "Dining Room", color: "#a9d8ee", labelColor: "#1f5f82", x: 360, y: 350, w: 190, h: 210 },
        { id: "laundry", label: "Laundry", color: "#a8c8ef", labelColor: "#25477f", x: 550, y: 350, w: 130, h: 210 },
        { id: "bathroom", label: "Bathroom", color: "#a7e2cf", labelColor: "#1c7560", x: 680, y: 350, w: 170, h: 210 },
      ],
    },
    floor002: {
      id: "floor002", homeId: "home001", name: "First Floor", image: "images/floor2.jpg",
      viewBox: "0 0 860 500",
      rooms: [
        { id: "bedroom2", label: "Master Bedroom", color: "#f3bcd4", labelColor: "#933c63", x: 20, y: 20, w: 440, h: 320 },
        { id: "bathroom2", label: "Bathroom", color: "#a7e2cf", labelColor: "#1c7560", x: 490, y: 20, w: 350, h: 320 },
        { id: "landing", label: "Landing", color: "#dde8f5", labelColor: "#4b6b9e", x: 20, y: 370, w: 820, h: 110 },
      ],
    },
    floor003: {
      id: "floor003", homeId: "home002", name: "Main Floor", image: "images/floor3.jpg",
      viewBox: "0 0 800 480",
      rooms: [
        { id: "living2", label: "Living Room", color: "#b7e6c0", labelColor: "#2e6b42", x: 30, y: 30, w: 350, h: 420 },
        { id: "kitchen2", label: "Kitchen", color: "#f7dd9b", labelColor: "#8b6a1c", x: 410, y: 30, w: 360, h: 420 },
      ],
    },
  };

  const devices = {
    /* ----- Ground Floor ----- */
    tv001: { homeId: "home001", floorId: "floor001", room: "media", type: "switch", name: "Media Room TV", icon: "📺", on: false, pos: { x: 40, y: 80 } },
    lamp001: { homeId: "home001", floorId: "floor001", room: "media", type: "switch", name: "Media Room Lamp", icon: "💡", on: false, pos: { x: 120, y: 150 } },

    camera001: { homeId: "home001", floorId: "floor001", room: "deck", type: "switch", name: "Deck Camera", icon: "📷", on: false, pos: { x: 490, y: 80 } },

    light001: { homeId: "home001", floorId: "floor001", room: "living", type: "switch", name: "Living Room Light", icon: "💡", on: false, pos: { x: 400, y: 185 } },
    speaker01: { homeId: "home001", floorId: "floor001", room: "living", type: "switch", name: "Living Room Speaker", icon: "🔊", on: false, pos: { x: 250, y: 310 } },
    sensor001: { homeId: "home001", floorId: "floor001", room: "living", type: "temp", name: "AC", image: "images/devices/AC.png", value: 26, pos: { x: 460, y: 320 } },

    fridge001: { homeId: "home001", floorId: "floor001", room: "kitchen", type: "switch", name: "Refrigerator", image: "images/devices/refrigerator.png", on: false, pos: { x: 700, y: 40 } },
    oven001: { homeId: "home001", floorId: "floor001", room: "kitchen", type: "switch", name: "Kitchen Oven", icon: "🍳", on: false, pos: { x: 820, y: 130 } },
    fan001: { homeId: "home001", floorId: "floor001", room: "kitchen", type: "switch", name: "Ceiling Fan", image: "images/devices/ceiling_fan.png", on: false, pos: { x: 750, y: 220 } },
    plug001: { homeId: "home001", floorId: "floor001", room: "kitchen", type: "switch", name: "Smart Plug", image: "images/devices/smart_plug.png", on: false, watts: 0, pos: { x: 810, y: 295 } },
    blinds001: { homeId: "home001", floorId: "floor001", room: "kitchen", type: "switch", name: "Blinds", icon: "🪟", on: false, pos: { x: 825, y: 70 } },

    light003: { homeId: "home001", floorId: "floor001", room: "bedroom", type: "switch", name: "Bedroom Light", icon: "💡", on: false, pos: { x: 135, y: 480 } },
    blinds004: { homeId: "home001", floorId: "floor001", room: "bedroom", type: "switch", name: "Bedroom Blinds", icon: "🪟", on: false, pos: { x: 30, y: 535 } },
    plug002: { homeId: "home001", floorId: "floor001", room: "bedroom", type: "switch", name: "Bedroom Plug", image: "images/devices/smart_plug.png", on: false, watts: 0, pos: { x: 30, y: 390 } },

    door001: { homeId: "home001", floorId: "floor001", room: "porch", type: "lock", name: "Door Lock", icon: "🔒", on: false, pos: { x: 290, y: 525 } },
    speaker04: { homeId: "home001", floorId: "floor001", room: "porch", type: "switch", name: "Porch Speaker", icon: "🔊", on: false, pos: { x: 325, y: 525 } },

    camera002: { homeId: "home001", floorId: "floor001", room: "porch", type: "switch", name: "porch Camera", image: "images/devices/camera.png", on: false, pos: { x: 300, y: 350 } },
    light004: { homeId: "home001", floorId: "floor001", room: "dining", type: "switch", name: "Dining Light", icon: "💡", on: false, pos: { x: 470, y: 410 } },
    fan002: { homeId: "home001", floorId: "floor001", room: "dining", type: "switch", name: "Dining Fan", image: "images/devices/ceiling_fan.png", on: false, pos: { x: 420, y: 520 } },

    washer001: { homeId: "home001", floorId: "floor001", room: "laundry", type: "switch", name: "Washing Machine", image: "images/devices/washing_machine.png", on: false, watts: 0, pos: { x: 615, y: 400 } },
    plug003: { homeId: "home001", floorId: "floor001", room: "laundry", type: "switch", name: "Iron", image: "images/devicesiron.png", on: false, watts: 0, pos: { x: 615, y: 520 } },

    //motion001:{ homeId:"home001", floorId:"floor001", room:"bathroom",type:"motion", name:"Bathroom Motion",      icon:"🚪", motion:false, pos:{x:730, y:390} },
    sensor002: { homeId: "home001", floorId: "floor001", room: "bathroom", type: "switch", name: "Bathroom Heater", image: "images/devices/Heater.png", value: 24, pos: { x: 800, y: 520 } },

    /* ----- First Floor ----- */
    fan003: { homeId: "home001", floorId: "floor002", room: "bedroom2", type: "switch", name: "Bedroom Fan", image: "images/devices/ceiling_fan.png", on: false, pos: { x: 240, y: 180 } },
    sensor003: { homeId: "home001", floorId: "floor002", room: "bedroom2", type: "temp", name: "Bedroom Temperature", icon: "🌡️", value: 26, pos: { x: 410, y: 100 } },
    motion002: { homeId: "home001", floorId: "floor002", room: "bathroom2", type: "motion", name: "Bathroom Motion", icon: "🚪", motion: false, pos: { x: 660, y: 180 } },

    /* ----- Apartment ----- */
    lightA: { homeId: "home002", floorId: "floor003", room: "living2", type: "switch", name: "Living Room Light", icon: "💡", on: false, pos: { x: 205, y: 240 } },
    plugA: { homeId: "home002", floorId: "floor003", room: "kitchen2", type: "switch", name: "Kitchen Smart Plug", icon: "⚡", on: false, watts: 0, pos: { x: 590, y: 240 } },
  };

  /* ============================================================
     2. NAVIGATION STATE
     ============================================================ */

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

  /* ← FIREBASE: replace body with firebase.database().ref(`devices/${id}`).update(patch) */
  function setDeviceState(id, patch) {
    if (!devices[id]) return;
    Object.assign(devices[id], patch);
    // Auto-calc wattage for smart plugs / washer
    if (["plug001", "plug002", "plug003", "plugA", "washer001"].includes(id)) {
      devices[id].watts = devices[id].on ? randomBetween(25, 110) : 0;
    }
    // Refresh the open modal if it shows this device
    const modal = document.getElementById("deviceModal");
    if (modal && modal.dataset.deviceId === id) openDeviceModal(id);
    // Refresh plan icon dot
    updatePlanIconDot(id);
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
        devices[id].value = randomBetween(18, 34);
        openDeviceModal(id); // refresh modal
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
    const msgs = ["simulated · local state", "syncing device tree", "simulated · local state", "heartbeat OK"];
    let i = 0;
    setInterval(() => { i = (i + 1) % msgs.length; el.textContent = msgs[i]; }, 3500);
  })();

  /* ============================================================
     BOOT
     ============================================================ */

  render();

})();

/*
 * ============================================================
 * HOW TO CONNECT TO FIREBASE REALTIME DATABASE
 * ============================================================
 *
 * 1) Add Firebase SDK (e.g. in index.html before script.js):
 *    <script type="module">
 *      import { initializeApp }        from "https://www.gstatic.com/firebasejs/10.x/firebase-app.js";
 *      import { getDatabase, ref, onValue, update }
 *                                      from "https://www.gstatic.com/firebasejs/10.x/firebase-database.js";
 *      const fbApp = initializeApp({ apiKey:"...", databaseURL:"..." });
 *      window._db = getDatabase(fbApp);
 *    </script>
 *
 * 2) Replace the static homes/floors/devices objects with listeners:
 *      onValue(ref(_db,"homes"),   s=>{ Object.assign(homes,   s.val()); render(); });
 *      onValue(ref(_db,"floors"),  s=>{ Object.assign(floors,  s.val()); render(); });
 *      onValue(ref(_db,"devices"), s=>{ Object.assign(devices, s.val());
 *                                        if(state.screen==="floorplan") renderFloorPlanScreen(); });
 *
 * 3) Replace setDeviceState body:
 *      function setDeviceState(id, patch){
 *        update(ref(_db, "devices/" + id), patch);
 *      }
 *    The onValue listener will re-render automatically.
 *
 * 4) Everything else stays the same.
 * ============================================================
 */