/*
 * DormHub 3D room map (Three.js r128 + OrbitControls, loaded from CDN before this file).
 *
 *   Dorm3D.mount({
 *     root: element,            // container with a fixed height
 *     dataUrl: '/explore/rooms',// JSON list of RoomView (see th.mfu.service.RoomView)
 *     mode: 'public' | 'admin',
 *     soonDays: 60,
 *     editUrl: '/admin/rooms/', // admin: link to the room form (+ id)
 *     bookingsUrl: '/admin/bookings',
 *     bookTemplate: element,    // public: <template> holding the booking form
 *     bookBlockedText: '...',   // public without bookTemplate: shown on free rooms instead of the form
 *     initialRoom: 'A103'       // optional room number to open first
 *   });
 *
 * Views: site (all buildings) -> building (rooms coloured by status) -> floor (upper floors lift away)
 *        -> room sheet (details, booking) -> room 3D (furnished model built from the room type).
 * Texts follow the page language through dhT('ไทย', 'English') from prefs.js.
 */
(function () {
    'use strict';

    var FH = 3.3;          // floor height
    var DEPTH = 12;        // building depth (two rows of rooms and a corridor)
    var CORRIDOR = 1.8;

    var COLORS = {
        ground: 0xeef2f7, lawn: 0xd6e8cf, path: 0xffffff, water: 0x9fd3e6,
        slab: 0xffffff, glass: 0xb9d4f2, edge: 0x8a96a8, neutral: 0xe6ebf2,
        tree: 0x8fbf7f, trunk: 0x9b8468,
        // status colours = design-system tokens --s-success / --s-soon / --s-info / --s-warning (app.css)
        AVAILABLE: 0x2e9a5a, SOON: 0xe8833a, OCCUPIED: 0x5b7a9d, MAINTENANCE: 0xd99a00,
        OCCUPIED_PUBLIC: 0xbcc5d2, select: 0x4f7a58
    };
    // colours of SJD Mansion (from the photos): cream centre building, white wings, green roof trim
    var SJD = {
        green: 0x2fa84f, cream: 0xf1e4c3, white: 0xf7f7f4, terracotta: 0xc8643c, steel: 0xc3cad2,
        glass: 0x6f8aa0, concrete: 0xd6d6d1, tank: 0x2f6fb5, column: 0xf4f2ed
    };
    var CSS_COLORS = { AVAILABLE: '#2e9a5a', SOON: '#e8833a', OCCUPIED: '#5b7a9d', MAINTENANCE: '#d99a00', OCCUPIED_PUBLIC: '#aab4c3' };

    function el(tag, cls, html) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (html != null) e.innerHTML = html;
        return e;
    }
    function esc(s) {
        return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }
    function baht(n) { return Number(n || 0).toLocaleString(dhT('th-TH', 'en-US'), { maximumFractionDigits: 0 }); }
    function thDate(iso) {
        if (!iso) return '-';
        var p = iso.split('-');
        return new Date(+p[0], +p[1] - 1, +p[2]).toLocaleDateString(dhT('th-TH', 'en-GB'), { day: 'numeric', month: 'short', year: 'numeric' });
    }
    // Lucide line icons, inlined because these buttons are built before the icon script runs
    // (the UI font has no arrow glyphs, and a text × does not sit on the type scale)
    var ICON_X = '<svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>';
    var ICON_BACK = '<svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 19-7-7 7-7"/><path d="M19 12H5"/></svg>';
    function damp(k, dt) { return 1 - Math.exp(-k * dt); }
    function smoother(t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    function easeInOut(t) { return t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
    var reduceMotion = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;

    function mount(opts) {
        var root = opts.root;
        var isAdmin = opts.mode === 'admin';
        var soonDays = opts.soonDays || 60;
        root.classList.add('d3-root');

        if (!window.THREE || !THREE.OrbitControls) {
            root.innerHTML = '<div class="d3-msg">' + dhT('โหลดไลบรารี 3D ไม่สำเร็จ ตรวจสอบการเชื่อมต่ออินเทอร์เน็ตแล้วลองใหม่', 'Couldn\'t load the 3D library. Check your internet connection and try again.') + '</div>';
            return;
        }

        // ---------- DOM ----------
        var host = el('div', 'd3-canvas');
        var labelsEl = el('div', 'd3-labels');
        var tip = el('div', 'd3-tip'); tip.hidden = true;
        var panel = el('aside', 'd3-panel');
        var crumbs = el('nav', 'd3-crumbs'); crumbs.setAttribute('aria-label', dhT('ตำแหน่ง', 'Location'));
        var pbody = el('div', 'd3-pbody');
        panel.appendChild(crumbs); panel.appendChild(pbody);
        var sheet = el('section', 'd3-sheet closed'); sheet.setAttribute('aria-live', 'polite');
        var shead = el('div', 'd3-shead');
        var closeBtn = el('button', 'btn btn-light btn-icon d3-x', ICON_X); closeBtn.setAttribute('aria-label', dhT('ปิด', 'Close'));
        var sheadWrap = el('div', 'd3-shead-wrap'); sheadWrap.appendChild(shead); sheadWrap.appendChild(closeBtn);
        var sbody = el('div', 'd3-sbody');
        sheet.appendChild(sheadWrap); sheet.appendChild(sbody);
        var backBtn = el('button', 'btn d3-back', ICON_BACK + ' ' + dhT('ย้อนกลับ', 'Back'));
        backBtn.type = 'button';
        backBtn.hidden = true;
        var loading = el('div', 'd3-msg', '<div class="spinner-border text-primary mb-2" role="status"></div><div>' + dhT('กำลังโหลดแผนผังหอพัก…', 'Loading the dorm map…') + '</div>');
        var roomView = el('div', 'd3-room3d'); roomView.hidden = true;
        [host, labelsEl, tip, backBtn, panel, sheet, roomView, loading].forEach(function (n) { root.appendChild(n); });

        // ---------- three.js ----------
        var renderer;
        try {
            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        } catch (e) {
            loading.innerHTML = dhT('เบราว์เซอร์นี้แสดงภาพ 3 มิติไม่ได้ ลองเปิดด้วย Chrome หรือ Safari รุ่นใหม่', 'This browser can\'t show 3D. Try a recent version of Chrome or Safari.');
            return;
        }
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        host.appendChild(renderer.domElement);

        var scene = new THREE.Scene();
        var camera = new THREE.PerspectiveCamera(38, 1, 0.5, 700);
        var controls = new THREE.OrbitControls(camera, renderer.domElement);
        controls.enableDamping = true;
        controls.dampingFactor = 0.07;
        controls.rotateSpeed = 0.6;
        controls.zoomSpeed = 0.8;
        controls.maxPolarAngle = 1.36;
        controls.minDistance = 10;
        controls.maxDistance = 230;

        scene.add(new THREE.HemisphereLight(0xffffff, 0xb8c2d0, 0.8));
        var sun = new THREE.DirectionalLight(0xffffff, 0.75);
        sun.position.set(-40, 80, 50);
        sun.castShadow = true;
        sun.shadow.mapSize.set(2048, 2048);
        sun.shadow.camera.left = -75; sun.shadow.camera.right = 75;
        sun.shadow.camera.top = 75; sun.shadow.camera.bottom = -75;
        sun.shadow.camera.far = 260;
        sun.shadow.bias = -0.0005;
        scene.add(sun);

        function std(color, extra) {
            var o = { color: color, roughness: 0.85, metalness: 0 };
            for (var k in extra) o[k] = extra[k];
            return new THREE.MeshStandardMaterial(o);
        }

        var state = { view: 'site', b: null, f: null, room: null, onlyBookable: !isAdmin };
        var buildings = [];      // { id, cx, cz, L, axis, front, floors:[{n, group, mats, y, op, rooms}], ... }
        var allRooms = [];       // RoomView + { mesh, pos, building }
        var roomMeshes = [];
        var labels = [];

        // ---------- data ----------
        // AJAX call to the room web service (GET /explore/rooms or /admin/layout/rooms), returns JSON
        $.ajax({
            url: opts.dataUrl,
            method: 'GET',
            dataType: 'json',
            success: function (data) { build(data); loading.remove(); start(); },
            error: function () {
                loading.innerHTML = dhT('โหลดข้อมูลห้องไม่สำเร็จ', 'Couldn\'t load the rooms.') + ' <a href="">' + dhT('ลองใหม่', 'Try again') + '</a>';
            }
        });

        function statusColor(s) {
            if (s === 'OCCUPIED' && !isAdmin) return COLORS.OCCUPIED_PUBLIC;
            return COLORS[s] || COLORS.neutral;
        }
        function statusCss(s) {
            if (s === 'OCCUPIED' && !isAdmin) return CSS_COLORS.OCCUPIED_PUBLIC;
            return CSS_COLORS[s] || '#9aa4b2';
        }
        function bookable(r) { return r.status === 'AVAILABLE' || r.status === 'SOON'; }

        // world position helpers: u along the building, v across (v > 0 = courtyard side)
        function toWorld(b, u, v) { return b.axis === 'x' ? [b.cx + u, b.cz + v * b.front] : [b.cx + v * b.front, b.cz + u]; }
        function dims(b, lu, lv) { return b.axis === 'x' ? [lu, lv] : [lv, lu]; }

        // ---------- SJD Mansion model ----------
        // Every building: 4 floors, a stair hall in the middle and 10 room bays along each face,
        // grouped 1 - 2 - 2 - [stair] - 2 - 2 - 1. Rooms x01-x05 / x06-x10 are in the first half
        // (courtyard side / back side), x11-x15 / x16-x20 in the second half.
        // Building A has rooms on floor 1 too; on B and C floor 1 has rooms only in the half next to A,
        // the other half is open (B: parking; C: office, laundry, parking).

        function canvasTex(w, h, draw, rx, ry) {
            var c = document.createElement('canvas');
            c.width = w; c.height = h;
            draw(c.getContext('2d'), w, h);
            var t = new THREE.CanvasTexture(c);
            t.wrapS = t.wrapT = THREE.RepeatWrapping;
            t.repeat.set(rx || 1, ry || 1);
            return t;
        }
        // cream lap siding (horizontal boards)
        var TEX = {};
        function sidingTex() {
            return canvasTex(64, 64, function (g, w, h) {
                g.fillStyle = '#efe3bf'; g.fillRect(0, 0, w, h);
                for (var y = 0; y < h; y += 8) { g.fillStyle = 'rgba(150,125,80,.35)'; g.fillRect(0, y, w, 1); g.fillStyle = 'rgba(255,255,255,.4)'; g.fillRect(0, y + 1, w, 1); }
            }, 1, 1);
        }
        // pebble-pattern floor tiles of the courtyard
        function pebbleTex(rx, ry) {
            return canvasTex(256, 256, function (g, w, h) {
                g.fillStyle = '#c9b8a6'; g.fillRect(0, 0, w, h);
                var cols = ['#e7ddd2', '#d6c6b5', '#bca792', '#ece6df', '#cdbca9', '#b39c86'];
                var seed = 7;
                function rnd() { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; }
                for (var i = 0; i < 260; i++) {
                    g.fillStyle = cols[i % cols.length];
                    g.beginPath();
                    g.ellipse(rnd() * w, rnd() * h, 6 + rnd() * 10, 5 + rnd() * 8, rnd() * 3, 0, Math.PI * 2);
                    g.fill();
                }
                g.strokeStyle = 'rgba(90,70,50,.35)';
                for (var x = 0; x <= w; x += 64) { g.beginPath(); g.moveTo(x, 0); g.lineTo(x, h); g.stroke(); g.beginPath(); g.moveTo(0, x); g.lineTo(w, x); g.stroke(); }
            }, rx, ry);
        }
        // white slatted privacy screen
        function slatTex() {
            return canvasTex(32, 64, function (g, w, h) {
                g.fillStyle = '#c9c3b6'; g.fillRect(0, 0, w, h);
                for (var y = 0; y < h; y += 8) { g.fillStyle = '#f4f1ea'; g.fillRect(0, y, w, 6); }
            }, 1, 1);
        }

        var ROOM_W = 4.0, STAIR_W = 4.4, PLINTH = 0.45;
        var BL = 10 * ROOM_W + STAIR_W;      // building length

        function bayU(bay) {
            return bay < 5 ? -BL / 2 + ROOM_W * (bay + 0.5) : -BL / 2 + 5 * ROOM_W + STAIR_W + ROOM_W * (bay - 5 + 0.5);
        }
        // room index 1-20 -> bay 0-9 and face (0 = courtyard side, 1 = back)
        function slotOf(idx) {
            if (idx <= 5) return { bay: idx - 1, side: 0 };
            if (idx <= 10) return { bay: 10 - idx, side: 1 };
            if (idx <= 15) return { bay: 5 + idx - 11, side: 0 };
            return { bay: 9 - (idx - 16), side: 1 };
        }
        // boundaries between bay groups get a full wall (fin); inside a pair only a thin column
        var FIN_AFTER = { 0: true, 2: true, 4: true, 6: true, 8: true };

        function build(data) {
            var map = {};
            data.forEach(function (r) {
                (map[r.building] = map[r.building] || {});
                (map[r.building][r.floor] = map[r.building][r.floor] || []).push(r);
            });
            ['A', 'B', 'C'].concat(Object.keys(map).sort()).forEach(function (id) {
                if (find(id)) return;
                var floorsMap = map[id] || {};
                var roomFloors = Object.keys(floorsMap).map(Number).sort(function (a, b) { return a - b; });
                var maxFloor = Math.max(4, roomFloors.length ? roomFloors[roomFloors.length - 1] : 0);
                buildings.push({ id: id, floorsMap: floorsMap, L: BL, floorCount: maxFloor, roomFloors: roomFloors, cols: 10 });
            });

            // U shape seen from the dorm entrance: A at the back, C on the left, B on the right
            var zA = -26, gap = 3, sideX = BL / 2 - DEPTH / 2, extra = 0;
            var wingZ = zA + DEPTH / 2 + gap + BL / 2;
            buildings.forEach(function (b) {
                if (b.id === 'A') { b.axis = 'x'; b.front = 1; b.cx = 0; b.cz = zA; b.kind = 'A'; }
                else if (b.id === 'B') { b.axis = 'z'; b.front = -1; b.cx = sideX; b.cz = wingZ; b.kind = 'B'; }
                else if (b.id === 'C') { b.axis = 'z'; b.front = 1; b.cx = -sideX; b.cz = wingZ; b.kind = 'C'; }
                else { b.axis = 'x'; b.front = -1; b.cx = (extra - 0.5) * (BL + 8); b.cz = wingZ + BL / 2 + 30; b.kind = 'X'; extra++; }
            });
            var site = { zA: zA, inner: sideX - DEPTH / 2, outer: sideX + DEPTH / 2, zFront: zA + DEPTH / 2, zWing0: zA + DEPTH / 2 + gap, zEnd: wingZ + BL / 2 };
            buildSite(site);
            buildings.forEach(buildBuilding);
            refreshColors(true);
        }
        function find(id) { for (var i = 0; i < buildings.length; i++) if (buildings[i].id === id) return buildings[i]; return null; }
        function wallColor(b) { return b.id === 'A' ? 0xf6f4ee : SJD.white; }

        // collects many copies of the same box and draws them as one InstancedMesh (keeps the scene fast)
        function Batch(group) { this.group = group; this.sets = {}; }
        Batch.prototype.add = function (key, geo, mat, x, y, z, ry) {
            var s = this.sets[key] || (this.sets[key] = { geo: geo, mat: mat, list: [] });
            s.list.push([x, y, z, ry || 0]);
        };
        Batch.prototype.flush = function (shadow) {
            var m4 = new THREE.Matrix4(), q = new THREE.Quaternion(), e = new THREE.Euler(), one = new THREE.Vector3(1, 1, 1), p = new THREE.Vector3();
            for (var k in this.sets) {
                var s = this.sets[k];
                var im = new THREE.InstancedMesh(s.geo, s.mat, s.list.length);
                s.list.forEach(function (t, i) { e.set(0, t[3], 0); q.setFromEuler(e); p.set(t[0], t[1], t[2]); m4.compose(p, q, one); im.setMatrixAt(i, m4); });
                im.castShadow = shadow !== false; im.receiveShadow = true;
                this.group.add(im);
            }
        };

        function car(x, z, rot, color) {
            var g = new THREE.Group();
            var body = new THREE.Mesh(new THREE.BoxGeometry(1.8, 0.7, 4.3), std(color, { roughness: 0.4, metalness: 0.3 }));
            body.position.y = 0.6;
            var cabin = new THREE.Mesh(new THREE.BoxGeometry(1.6, 0.55, 2.3), std(0x3b4652, { roughness: 0.2 }));
            cabin.position.set(0, 1.2, -0.2);
            body.castShadow = cabin.castShadow = true;
            g.add(body); g.add(cabin);
            g.position.set(x, 0, z);
            g.rotation.y = rot;
            return g;
        }
        function palm(x, z, h) {
            var g = new THREE.Group();
            var trunk = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.14, h, 6), std(0x9b8468));
            trunk.position.y = h / 2;
            g.add(trunk);
            var leafM = std(0x4f8f3a, { side: THREE.DoubleSide });
            for (var i = 0; i < 7; i++) {
                var leaf = new THREE.Mesh(new THREE.PlaneGeometry(0.5, 2.2), leafM);
                leaf.position.set(0, h, 0);
                leaf.rotation.set(-1.0, i * Math.PI * 2 / 7, 0, 'YXZ');
                leaf.translateY(1.0);
                leaf.castShadow = true;
                g.add(leaf);
            }
            g.position.set(x, 0, z);
            return g;
        }
        function plane(w, d, mat, x, y, z) {
            var m = new THREE.Mesh(new THREE.PlaneGeometry(w, d), mat);
            m.rotation.x = -Math.PI / 2;
            m.position.set(x, y, z);
            m.receiveShadow = true;
            scene.add(m);
            return m;
        }
        function sbox(w, h, d, mat, x, y, z) {
            var m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), mat);
            m.position.set(x, y, z);
            m.castShadow = m.receiveShadow = true;
            scene.add(m);
            return m;
        }
        function siteLabel(text, x, y, z, show) {
            addLabel(el('span', 'd3-lbl d3-lbl-site', text), new THREE.Vector3(x, y, z), show || function () { return state.view === 'site'; }, 'center');
        }

        function buildSite(s) {
            plane(400, 400, std(0xdfe5dc, { roughness: 1 }), 0, 0, 0);
            var asphalt = std(0x8d9196, { roughness: 1 }), concrete = std(SJD.concrete, { roughness: 1 });
            var x0 = -s.outer - 9, x1 = s.outer + 9, zBack = s.zA - DEPTH / 2 - 9, zRoad = s.zEnd + 10;

            // service roads behind A, B and C, and the main road in front of the dorm
            plane(x1 - x0, 6, asphalt, 0, 0.01, s.zA - DEPTH / 2 - 3.5);
            plane(6, zRoad - zBack, asphalt, -s.outer - 3.5, 0.01, (zBack + zRoad) / 2);
            plane(6, zRoad - zBack, asphalt, s.outer + 3.5, 0.01, (zBack + zRoad) / 2);
            plane(x1 - x0 + 40, 8, asphalt, 0, 0.012, zRoad + 4);

            // courtyard: pebble tiles near A (tables), then the open-air car park towards the entrance
            var plazaLen = (s.zEnd - s.zWing0) * 0.55;
            var plaza = plane(s.inner * 2, plazaLen + 3, std(0xffffff, { map: pebbleTex(s.inner / 2, (plazaLen + 3) / 4), roughness: 0.9 }), 0, 0.02, s.zFront + (plazaLen + 3) / 2);
            plane(BL, gap(), std(0xffffff, { map: pebbleTex(BL / 4, 1), roughness: 0.9 }), 0, 0.02, s.zFront + 1.5);
            function gap() { return 3; }
            var parkZ0 = s.zFront + plazaLen + 3, parkLen = zRoad - parkZ0;
            plane(s.inner * 2 + DEPTH * 2 + 14, parkLen, concrete, 0, 0.015, parkZ0 + parkLen / 2);

            // tables with benches in the plaza (two rows)
            var tableM = std(0xe7ece6), benchM = std(0xcfe0c8);
            var batch = new Batch(scene);
            var tg = new THREE.CylinderGeometry(0.55, 0.5, 0.08, 16), tl = new THREE.CylinderGeometry(0.15, 0.25, 0.7, 10), bg = new THREE.BoxGeometry(1.2, 0.08, 0.35), bl = new THREE.BoxGeometry(0.25, 0.42, 0.3);
            for (var row = 0; row < 5; row++) {
                [-1, 1].forEach(function (sx) {
                    var x = sx * (s.inner - 3.2), z = s.zFront + 4 + row * (plazaLen - 4) / 4;
                    batch.add('tt', tg, tableM, x, 0.75, z);
                    batch.add('tl', tl, tableM, x, 0.36, z);
                    [-1, 1].forEach(function (bz) {
                        batch.add('bs', bg, benchM, x, 0.45, z + bz * 0.9);
                        batch.add('bl', bl, benchM, x - 0.4, 0.21, z + bz * 0.9);
                        batch.add('bl', bl, benchM, x + 0.4, 0.21, z + bz * 0.9);
                    });
                });
            }
            // green planters with red flowers along both wings
            var potM = std(0x2f9e4a), flowerM = std(0xc8452f, { roughness: 1 }), leafM = std(0x4f8a3a, { roughness: 1 });
            var pg = new THREE.BoxGeometry(0.7, 0.5, 1.8), fg = new THREE.SphereGeometry(0.5, 8, 6);
            for (var z = s.zWing0 + 3; z < s.zFront + plazaLen; z += 4.4) {
                [-1, 1].forEach(function (sx) {
                    var x = sx * (s.inner - 1.0);
                    batch.add('pot', pg, potM, x, 0.25, z);
                    batch.add('fl', fg, (Math.round(z) % 2) ? flowerM : leafM, x, 0.65, z - 0.4);
                    batch.add('fl2', fg, flowerM, x, 0.62, z + 0.4);
                });
            }
            batch.flush();
            [[-1, 0.15], [1, 0.4], [-1, 0.7], [1, 0.9]].forEach(function (p) { scene.add(palm(p[0] * (s.inner - 1.6), s.zFront + 2 + p[1] * plazaLen, 3.2)); });

            // cars in the open-air car park
            [[-4.5, 0.25, 0xc3c7cc], [-1.5, 0.25, 0x26292d], [1.5, 0.25, 0xf0f0ee], [4.5, 0.25, 0x9aa3ad], [-3, 0.65, 0x2b2f34], [3, 0.65, 0xc3c7cc]].forEach(function (c) {
                scene.add(car(c[0], parkZ0 + 3 + c[1] * (parkLen - 6), 0, c[2]));
            });

            // green fence around the site, open at the entrance (left side, in front of building C)
            var fenceM = std(0x2f9e4a, { roughness: 0.6 });
            var fz = zRoad - 0.5, gateX0 = -s.outer + 1, gateX1 = -s.inner + 1;
            sbox(gateX0 - x0, 1.4, 0.12, fenceM, (x0 + gateX0) / 2, 0.7, fz);
            sbox(x1 - gateX1, 1.4, 0.12, fenceM, (gateX1 + x1) / 2, 0.7, fz);
            sbox(0.12, 1.4, zRoad - zBack, fenceM, x0, 0.7, (zBack + zRoad) / 2);
            sbox(0.12, 1.4, zRoad - zBack, fenceM, x1, 0.7, (zBack + zRoad) / 2);
            sbox(x1 - x0, 1.4, 0.12, fenceM, 0, 0.7, zBack);

            // motorcycle shelters with green roofs along the back fences
            var roofM = std(0x2f9e4a, { roughness: 0.5 }), postM = std(0xdddddd), bikeM = std(0x3a3d42);
            var sb = new Batch(scene);
            function shelter(axis, fixed, from, to) {
                var len = to - from, mid = (from + to) / 2;
                var roof = axis === 'x' ? new THREE.BoxGeometry(len, 0.08, 2.6) : new THREE.BoxGeometry(2.6, 0.08, len);
                sb.add('roof' + axis + len, roof, roofM, axis === 'x' ? mid : fixed, 2.5, axis === 'x' ? fixed : mid);
                var pgeo = new THREE.BoxGeometry(0.1, 2.5, 0.1), bgeo = axis === 'x' ? new THREE.BoxGeometry(0.4, 0.9, 1.6) : new THREE.BoxGeometry(1.6, 0.9, 0.4);
                for (var t = from; t <= to; t += 4) sb.add('post', pgeo, postM, axis === 'x' ? t : fixed, 1.25, axis === 'x' ? fixed : t);
                for (var t2 = from + 1; t2 < to; t2 += 0.9) sb.add('bike' + axis, bgeo, bikeM, axis === 'x' ? t2 : fixed, 0.45, axis === 'x' ? fixed : t2);
            }
            shelter('x', zBack + 1.6, x0 + 8, x1 - 8);
            shelter('z', x0 + 1.6, zBack + 8, zRoad - 12);
            shelter('z', x1 - 1.6, zBack + 8, zRoad - 12);
            sb.flush();

            // spirit house at the entrance end of building C
            var shx = -s.outer - 1.2, shz = s.zEnd + 2.5;
            sbox(1.6, 0.8, 1.6, std(0xd9d2c4), shx, 0.4, shz);
            sbox(0.25, 1.2, 0.25, std(0xc9a227, { metalness: 0.5, roughness: 0.35 }), shx, 1.4, shz);
            var house = sbox(0.9, 0.6, 0.9, std(0xd4af37, { metalness: 0.6, roughness: 0.3 }), shx, 2.3, shz);
            var hr = new THREE.Mesh(new THREE.ConeGeometry(0.75, 0.7, 4), std(0xc9a227, { metalness: 0.6, roughness: 0.3 }));
            hr.position.set(shx, 2.95, shz);
            hr.rotation.y = Math.PI / 4;
            scene.add(hr);

            siteLabel(dhT('ลานส่วนกลาง', 'Courtyard'), 0, 0.6, s.zFront + plazaLen / 2);
            siteLabel(dhT('ลานจอดรถ', 'Car park'), 0, 0.6, parkZ0 + parkLen / 2);
            siteLabel(dhT('ทางเข้าหอ', 'Entrance'), (gateX0 + gateX1) / 2, 0.6, zRoad + 2);
            siteLabel(dhT('ศาลพระภูมิ', 'Spirit house'), shx, 3.6, shz);
        }

        function signTexture(top, bottom) {
            var c = document.createElement('canvas');
            c.width = 512; c.height = 320;
            var g = c.getContext('2d');
            g.fillStyle = '#2fa84f'; g.fillRect(0, 0, 512, 320);
            g.fillStyle = '#ffffff'; g.fillRect(28, 28, 456, 264);
            function text() {
                g.fillStyle = '#2fa84f'; g.fillRect(40, 40, 432, 240);
                g.fillStyle = '#ffffff';
                g.textAlign = 'center'; g.textBaseline = 'middle';
                g.font = '600 150px "Anuphan", sans-serif';
                g.fillText(top, 256, 135);
                g.font = '600 64px "Anuphan", sans-serif';
                g.fillText(bottom, 256, 240);
            }
            text();
            var texture = new THREE.CanvasTexture(c);
            // the web font may still be loading: redraw the sign once it is there
            if (document.fonts && document.fonts.load) {
                document.fonts.load('600 64px "Anuphan"', top + bottom).then(function () {
                    text();
                    texture.needsUpdate = true;
                });
            }
            return texture;
        }

        function buildBuilding(b) {
            b.group = new THREE.Group();
            b.op = 1; b.opTarget = 1;
            b.floors = [];
            var c = toWorld(b, 0, 0);
            b.center = new THREE.Vector3(c[0], 0, c[1]);
            var wall = wallColor(b);
            var half = DEPTH / 2, RECESS = 1.2;
            var rdUp = half - CORRIDOR / 2 - RECESS;   // room depth on floors 2-4 (balcony recessed into the facade)
            var rdG = half - CORRIDOR / 2 - 0.25;       // room depth on floor 1
            var stairU = (bayU(4) + bayU(5)) / 2;
            var openHalf = b.kind === 'B' || b.kind === 'C';  // floor 1: only bays 0-4 have rooms

            function geo(lu, h, lv) { var d = dims(b, lu, lv); return new THREE.BoxGeometry(d[0], h, d[1]); }
            function W(u, v) { return toWorld(b, u, v); }
            var G = {
                slab: geo(BL + 0.3, 0.3, DEPTH + 0.3),
                fin: geo(0.3, FH, RECESS + 0.05),
                col: geo(0.18, FH - 0.3, 0.18),
                balc: geo(ROOM_W - 0.2, 0.12, RECESS),
                rail: geo(ROOM_W - 0.3, 0.04, 0.04),
                door: geo(ROOM_W * 0.5, 2.1, 0.05),
                ac: geo(0.75, 0.5, 0.3),
                end: new THREE.BoxGeometry(b.axis === 'x' ? 0.3 : DEPTH, FH, b.axis === 'x' ? DEPTH : 0.3),
                stairFace: geo(STAIR_W, FH, 0.25),
                glassBlock: geo(1.4, 1.3, 0.08),
                gWall: geo(ROOM_W - 0.1, FH - PLINTH - 0.3, 0.12),
                gDoor: geo(0.9, 2.05, 0.08),
                gWin: geo(1.0, 0.9, 0.08),
                step: geo(1.3, 0.15, 0.32),
                screen: geo(1.5, 1.6, 0.06),
                planter: geo(1.2, 0.45, 0.45),
                column: new THREE.BoxGeometry(0.5, FH - 0.3, 0.5),
                colBase: new THREE.BoxGeometry(0.62, 0.9, 0.62),
                colTop: new THREE.BoxGeometry(0.55, 0.9, 0.55)
            };
            var siding = sidingTex();

            for (var f = 1; f <= b.floorCount; f++) {
                var g = new THREE.Group();
                var y0 = (f - 1) * FH;
                var M = {
                    wall: std(wall, { transparent: true }),
                    siding: std(0xffffff, { map: siding, transparent: true }),
                    terracotta: std(SJD.terracotta, { transparent: true }),
                    steel: std(SJD.steel, { transparent: true, metalness: 0.6, roughness: 0.3 }),
                    glass: std(SJD.glass, { transparent: true, roughness: 0.15 }),
                    glassBlock: std(0xbfd9e6, { transparent: true, roughness: 0.2 }),
                    ac: std(0xe4e7ea, { transparent: true }),
                    door: std(0xe9dfc3, { transparent: true }),
                    step: std(0xa86a45, { transparent: true }),
                    screen: std(0xffffff, { map: slatTex(), transparent: true }),
                    green: std(0x2f9e4a, { transparent: true }),
                    column: std(SJD.column, { transparent: true })
                };
                var mats = Object.keys(M).map(function (k) { return M[k]; });
                var B = new Batch(g);
                var list = b.floorsMap[f] || [];
                var ground = f === 1;

                // floor slab (floor 1: a plinth under the room half, a thin slab under the open half)
                if (ground && openHalf) {
                    var hl = BL / 2 + STAIR_W / 2;
                    var pp = W(-BL / 2 + hl / 2, 0);
                    var pl = new THREE.Mesh(geo(hl, PLINTH, DEPTH), M.wall);
                    pl.position.set(pp[0], PLINTH / 2, pp[1]);
                    pl.receiveShadow = true;
                    g.add(pl);
                } else {
                    var sp = W(0, 0);
                    B.add('slab', ground ? geo(BL, PLINTH, DEPTH) : G.slab, M.wall, sp[0], ground ? PLINTH / 2 : y0 + 0.15, sp[1]);
                }

                // both short ends: cream siding with a tall glass-block window
                [-1, 1].forEach(function (e) {
                    if (ground && openHalf && e > 0) return;
                    var p = W(e * (BL / 2 - 0.15), 0);
                    B.add('end', G.end, M.siding, p[0], y0 + FH / 2, p[1]);
                    var gw = W(e * (BL / 2 + 0.02), 0);
                    B.add('gbEnd', geo(0.08, 1.6, 0.9), M.glassBlock, gw[0], y0 + 1.7, gw[1], 0);
                });

                // stair hall in the middle of both faces: cream siding + glass blocks
                [1, -1].forEach(function (s) {
                    var p = W(stairU, s * (half - 0.12));
                    B.add('stair', G.stairFace, M.siding, p[0], y0 + FH / 2, p[1]);
                    if (!(ground && s > 0)) {
                        var q = W(stairU, s * (half + 0.02));
                        B.add('gb', G.glassBlock, M.glassBlock, q[0], y0 + 1.9, q[1]);
                    }
                });

                if (ground) buildGround(b, g, B, M, G, list, openHalf, stairU);
                else {
                    // floors 2-4: recessed balconies with stainless railings, AC units and sliding doors
                    for (var bay = 0; bay < 10; bay++) {
                        var u = bayU(bay);
                        [1, -1].forEach(function (s) {
                            var vb = s * (half - RECESS / 2);
                            var p = W(u, vb);
                            B.add('balc', G.balc, M.wall, p[0], y0 + 0.36, p[1]);
                            [0.55, 0.85, 1.15].forEach(function (h) { var r = W(u, s * (half - 0.05)); B.add('rail', G.rail, M.steel, r[0], y0 + 0.3 + h, r[1]); });
                            var d = W(u - 0.5, s * (half - RECESS - 0.03));
                            B.add('door', G.door, M.glass, d[0], y0 + 0.3 + 1.05, d[1]);
                            var a = W(u + ROOM_W / 2 - 0.6, s * (half - RECESS + 0.35));
                            B.add('ac', G.ac, M.ac, a[0], y0 + 0.67, a[1]);
                            // wall above the balcony opening
                            var top = W(u, s * (half - 0.1));
                            B.add('lintel', geo(ROOM_W, 0.5, 0.2), M.wall, top[0], y0 + FH - 0.25, top[1]);
                        });
                        [1, -1].forEach(function (s) {
                            var fu = bayU(bay) + ROOM_W / 2;
                            if (bay === 9) return;
                            var fp = W(fu, s * (half - RECESS / 2));
                            if (FIN_AFTER[bay] === true) B.add('fin', G.fin, M.wall, fp[0], y0 + FH / 2, fp[1]);
                            else if (FIN_AFTER[bay] === undefined) B.add('col', G.col, M.wall, fp[0], y0 + 0.3 + (FH - 0.3) / 2, fp[1]);
                        });
                        if (bay === 5) [1, -1].forEach(function (s) { var fp = W(bayU(5) - ROOM_W / 2, s * (half - RECESS / 2)); B.add('fin', G.fin, M.wall, fp[0], y0 + FH / 2, fp[1]); });
                    }
                    [1, -1].forEach(function (s) {
                        [-1, 1].forEach(function (e) { var fp = W(e * (BL / 2 - 0.15), s * (half - RECESS / 2)); B.add('fin', G.fin, M.wall, fp[0], y0 + FH / 2, fp[1]); });
                    });
                }

                // rooms (coloured by status when the building is selected)
                list.forEach(function (r, j) {
                    var idx = parseInt(String(r.number).slice(-2), 10);
                    if (!(idx >= 1 && idx <= 20)) idx = j + 1;
                    var slot = slotOf(idx);
                    var u = bayU(slot.bay), s = slot.side === 0 ? 1 : -1;
                    var rd = ground ? rdG : rdUp;
                    var v = s * (CORRIDOR / 2 + rd / 2);
                    var p = W(u, v);
                    var d = dims(b, ROOM_W - 0.24, rd - 0.1);
                    var h = ground ? FH - PLINTH - 0.35 : FH - 0.65;
                    var rgeo = new THREE.BoxGeometry(d[0], h, d[1]);
                    rgeo.translate(0, h / 2, 0);
                    var mesh = new THREE.Mesh(rgeo, std(wall, { transparent: true, roughness: 0.7 }));
                    mesh.position.set(p[0], ground ? PLINTH : y0 + 0.3, p[1]);
                    mesh.castShadow = mesh.receiveShadow = true;
                    mesh.userData.room = r;
                    r.mesh = mesh; r.side = slot.side; r.target = new THREE.Color(wall); r.hover = 0;
                    r.pos = new THREE.Vector3(p[0], (ground ? PLINTH : y0 + 0.3) + h + 0.4, p[1]);
                    r.b = b;
                    g.add(mesh);
                    roomMeshes.push(mesh);
                    allRooms.push(r);
                    if (r.status !== 'MAINTENANCE' || isAdmin) {
                        var tag = el('span', 'd3-lbl d3-lbl-room', esc(r.number));
                        r.tag = tag;
                        addLabel(tag, r.pos, (function (ff) {
                            return function () { return state.view === 'floor' && state.b === b.id && state.f === ff; };
                        })(f), 'center');
                    }
                });

                B.flush();
                g.userData = { n: f, y: 0, yTarget: 0, op: 1, opTarget: 1 };
                b.floors.push({ n: f, group: g, mats: mats, rooms: list });
                b.group.add(g);

                if (list.length) {
                    var chip = el('button', 'd3-lbl d3-lbl-floor', dhT('ชั้น ', 'Floor ') + f);
                    chip.type = 'button';
                    chip.addEventListener('click', (function (ff) { return function () { goFloor(ff); }; })(f));
                    var cp = W(BL / 2 + 1.2, half + 0.5);
                    (function (ff, chipEl, pos) {
                        var lb = addLabel(chipEl, pos, function () {
                            return state.view !== 'site' && state.b === b.id && (state.f == null || ff <= state.f);
                        }, 'left');
                        lb.after = function () { chipEl.classList.toggle('active', state.f === ff); };
                    })(f, chip, new THREE.Vector3(cp[0], y0 + 1.7, cp[1]));
                }
            }

            buildRoof(b, stairU);
            b.group.traverse(function (o) { o.userData.building = b.id; });
            scene.add(b.group);

            var tag = el('button', 'd3-lbl d3-lbl-building');
            tag.type = 'button';
            tag.addEventListener('click', function () { goBuilding(b.id); });
            b.tag = tag;
            addLabel(tag, b.center.clone().setY(b.floorCount * FH + 6), function () { return state.view === 'site'; }, 'top');
        }

        // floor 1: rooms with their own outside doors, tiled steps, slatted screens and planters;
        // the stair hall entrance with round columns; on B and C the open half with parking (and office + laundry on C)
        function buildGround(b, g, B, M, G, list, openHalf, stairU) {
            var half = DEPTH / 2;
            function geo(lu, h, lv) { var d = dims(b, lu, lv); return new THREE.BoxGeometry(d[0], h, d[1]); }
            function W(u, v) { return toWorld(b, u, v); }
            var roomBays = openHalf ? [0, 1, 2, 3, 4] : [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
            roomBays.forEach(function (bay) {
                var u = bayU(bay);
                [1, -1].forEach(function (s) {
                    var w = W(u, s * (half - 0.06));
                    B.add('gwall', G.gWall, M.terracotta, w[0], PLINTH + (FH - PLINTH - 0.3) / 2, w[1]);
                    var d = W(u - 0.8, s * (half + 0.01));
                    B.add('gdoor', G.gDoor, M.door, d[0], PLINTH + 1.03, d[1]);
                    var wn = W(u + 0.6, s * (half + 0.01));
                    B.add('gwin', G.gWin, M.glass, wn[0], PLINTH + 1.5, wn[1]);
                    for (var k = 0; k < 3; k++) {
                        var st = W(u - 0.8, s * (half + 0.16 + k * 0.32));
                        B.add('step', G.step, M.step, st[0], PLINTH - 0.075 - k * 0.15, st[1]);
                    }
                    var sc = W(u + 1.1, s * (half + 0.9));
                    B.add('screen', G.screen, M.screen, sc[0], 0.8, sc[1]);
                    if (bay % 2 === 0) { var pl = W(u + 0.4, s * (half + 1.45)); B.add('planter', G.planter, M.green, pl[0], 0.22, pl[1]); }
                });
            });

            // stair hall entrance facing the courtyard: flat canopy on four round columns, steps, glass door
            var cv = half + 2.2;
            var canopy = W(stairU, cv - 0.4);
            B.add('canopy', geo(STAIR_W + 1.6, 0.25, 3.6), M.wall, canopy[0], 3.0, canopy[1]);
            var colGeo = new THREE.CylinderGeometry(0.25, 0.25, 2.9, 14), baseGeo = new THREE.CylinderGeometry(0.38, 0.42, 0.8, 14);
            [-1, 1].forEach(function (e) {
                [cv + 0.9, cv - 1.4].forEach(function (v) {
                    var p = W(stairU + e * (STAIR_W / 2 + 0.3), v);
                    B.add('rcol', colGeo, M.column, p[0], 1.45, p[1]);
                    B.add('rbase', baseGeo, M.terracotta, p[0], 0.4, p[1]);
                });
            });
            for (var k = 0; k < 3; k++) { var st = W(stairU, half + 0.4 + k * 0.4); B.add('estep', geo(STAIR_W - 0.4, 0.15, 0.4), M.wall, st[0], PLINTH - 0.075 - k * 0.15, st[1]); }
            var gd = W(stairU, half + 0.02);
            B.add('edoor', geo(2.2, 2.3, 0.08), M.glass, gd[0], PLINTH + 1.15, gd[1]);

            if (!openHalf) return;
            // open half: square columns (white, terracotta base, cream top) and cars
            var u0 = bayU(5) - ROOM_W / 2, u1 = BL / 2;
            for (var bay = 5; bay <= 10; bay++) {
                var cu = bay === 10 ? u1 - 0.3 : bayU(bay) - ROOM_W / 2;
                [half - 0.3, -half + 0.3, 0].forEach(function (v) {
                    if (v === 0 && bay % 2) return;
                    var p = W(cu, v);
                    B.add('column', G.column, M.column, p[0], (FH - 0.3) / 2, p[1]);
                    B.add('colBase', G.colBase, M.terracotta, p[0], 0.45, p[1]);
                    B.add('colTop', G.colTop, M.siding, p[0], FH - 0.75, p[1]);
                });
            }
            var ground = W((u0 + u1) / 2, 0);
            var floor = new THREE.Mesh(geo(u1 - u0, 0.05, DEPTH), M.ac);
            floor.position.set(ground[0], 0.03, ground[1]);
            floor.receiveShadow = true;
            g.add(floor);
            var rot = b.axis === 'x' ? Math.PI / 2 : 0;
            var carBays = b.kind === 'C' ? [8, 9] : [5, 6, 7, 8, 9];
            var colors = [0xf0f0ee, 0x2b2f34, 0xc3c7cc, 0x7d8590, 0xf0f0ee];
            carBays.forEach(function (bay, i) {
                [half / 2, -half / 2].forEach(function (v, k) {
                    if ((bay + k) % 3 === 0) return;
                    var p = W(bayU(bay), v);
                    g.add(car(p[0], p[1], rot, colors[(i + k) % colors.length]));
                });
            });
            if (b.kind === 'C') {
                // office next to the entrance (cream walls, windows, a green wall) and the laundry with washers
                var o = W((bayU(5) + bayU(6)) / 2, 0);
                var office = new THREE.Mesh(geo(ROOM_W * 2 - 0.4, FH - 0.35, DEPTH - 1.2), M.siding);
                office.position.set(o[0], (FH - 0.35) / 2, o[1]);
                office.castShadow = office.receiveShadow = true;
                g.add(office);
                var ow = W((bayU(5) + bayU(6)) / 2, half - 0.55);
                B.add('owin', geo(ROOM_W * 1.2, 1.1, 0.08), M.glass, ow[0], 1.6, ow[1]);
                var gw = W(bayU(6) + 0.8, half - 0.56);
                B.add('ogreen', geo(1.6, FH - 0.4, 0.06), M.green, gw[0], (FH - 0.4) / 2, gw[1]);
                var lu = bayU(7);
                [-1, 1].forEach(function (s) {
                    var fr = W(lu, s * 2.2);
                    B.add('lframe', geo(ROOM_W - 0.4, 0.08, 1.2), M.green, fr[0], 1.6, fr[1]);
                    for (var m = 0; m < 4; m++) {
                        var wp = W(lu - 1.3 + m * 0.85, s * 2.2);
                        B.add('washer', geo(0.65, 0.95, 0.65), M.ac, wp[0], 0.48, wp[1]);
                    }
                    [-1, 1].forEach(function (e) { var pp = W(lu + e * (ROOM_W / 2 - 0.25), s * 2.2); B.add('lpost', geo(0.08, 1.6, 0.08), M.green, pp[0], 0.8, pp[1]); });
                });
                var lbl = W(bayU(5) + ROOM_W, half + 1);
                siteLabel(dhT('สำนักงาน', 'Office'), o[0], 3.4, o[1], function () { return state.view === 'site' || state.b === 'C'; });
                var lp = W(lu, 0);
                siteLabel(dhT('ที่ซักผ้า', 'Laundry'), lp[0], 2.6, lp[1], function () { return state.view === 'site' || state.b === 'C'; });
            }
            var pk = W(bayU(b.kind === 'C' ? 8 : 7) + 1, 0);
            siteLabel(dhT('ที่จอดรถใต้ตึก', 'Parking under the building'), pk[0], 2.4, pk[1], function () { return state.b === b.id; });
        }

        // roof: green parapet stepped up at both ends and at the stair; A has the SJD sign tower,
        // B and C have open green frames with blue water tanks over the stair and at the road end
        function buildRoof(b, stairU) {
            var roof = new THREE.Group();
            var half = DEPTH / 2;
            function geo(lu, h, lv) { var d = dims(b, lu, lv); return new THREE.BoxGeometry(d[0], h, d[1]); }
            function put(geom, mat, u, y, v) { var p = toWorld(b, u, v); var m = new THREE.Mesh(geom, mat); m.position.set(p[0], y, p[1]); m.castShadow = true; m.receiveShadow = true; roof.add(m); return m; }
            var RM = {
                wall: std(wallColor(b), { transparent: true }),
                green: std(SJD.green, { transparent: true }),
                tank: std(SJD.tank, { transparent: true, roughness: 0.4 }),
                siding: std(0xffffff, { map: sidingTex(), transparent: true })
            };
            b.roofMats = Object.keys(RM).map(function (k) { return RM[k]; });
            put(geo(BL + 0.4, 0.3, DEPTH + 0.4), RM.wall, 0, 0.15, 0);
            [half + 0.2, -half - 0.2].forEach(function (v) { put(geo(BL + 0.6, 0.55, 0.22), RM.green, 0, 0.55, v); });
            [-1, 1].forEach(function (e) {
                put(geo(0.22, 0.55, DEPTH + 0.6), RM.green, e * (BL / 2 + 0.2), 0.55, 0);
                [half + 0.2, -half - 0.2].forEach(function (v) { put(geo(ROOM_W + 0.4, 0.5, 0.26), RM.green, e * (BL / 2 - ROOM_W / 2), 1.05, v); });
            });
            function tanks(u, v, y, n) {
                for (var i = 0; i < n; i++) {
                    var t = new THREE.Mesh(new THREE.CylinderGeometry(0.55, 0.55, 1.3, 16), RM.tank);
                    var p = toWorld(b, u + (i - (n - 1) / 2) * 1.3, v);
                    t.position.set(p[0], y + 0.65, p[1]);
                    t.castShadow = true;
                    roof.add(t);
                }
            }
            function frame(u, len, h, wid) {
                [-1, 1].forEach(function (su) { [-1, 1].forEach(function (sv) { put(geo(0.2, h, 0.2), RM.green, u + su * len / 2, h / 2 + 0.3, sv * wid / 2); }); });
                put(geo(len + 0.3, 0.25, 0.25), RM.green, u, h + 0.3, wid / 2);
                put(geo(len + 0.3, 0.25, 0.25), RM.green, u, h + 0.3, -wid / 2);
                put(geo(0.25, 0.25, wid + 0.3), RM.green, u - len / 2, h + 0.3, 0);
                put(geo(0.25, 0.25, wid + 0.3), RM.green, u + len / 2, h + 0.3, 0);
                put(geo(len, 0.12, wid), RM.green, u, h + 0.5, 0);
                [0.6, 1.0].forEach(function (rh) {
                    put(geo(len, 0.04, 0.04), RM.green, u, h + 0.5 + rh, wid / 2);
                    put(geo(len, 0.04, 0.04), RM.green, u, h + 0.5 + rh, -wid / 2);
                });
            }
            if (b.kind === 'A') {
                // stair tower above the roof with the SJD Mansion sign facing the courtyard
                put(geo(STAIR_W + 0.6, 3.6, DEPTH * 0.6), RM.siding, stairU, 2.1, 0);
                put(geo(STAIR_W + 1.0, 0.3, DEPTH * 0.6 + 0.4), RM.green, stairU, 4.0, 0);
                var cap = new THREE.Mesh(new THREE.ConeGeometry(STAIR_W * 0.75, 1.2, 4), RM.green);
                var cp = toWorld(b, stairU, 0);
                cap.position.set(cp[0], 4.75, cp[1]);
                cap.rotation.y = Math.PI / 4;
                cap.scale.set(1, 1, DEPTH * 0.6 / STAIR_W);
                roof.add(cap);
                var sign = new THREE.Mesh(new THREE.PlaneGeometry(3.8, 2.4), new THREE.MeshBasicMaterial({ map: signTexture(opts.signTop || 'SJD', opts.signBottom || 'แมนชั่น'), transparent: true }));
                b.roofMats.push(sign.material);
                var sp = toWorld(b, stairU, DEPTH * 0.3 + 0.02);
                sign.position.set(sp[0], 2.3, sp[1]);
                if (b.front < 0) sign.rotation.y = Math.PI;
                roof.add(sign);
                tanks(stairU - STAIR_W - 1.2, -1.5, 0.3, 2);
                tanks(stairU + STAIR_W + 1.2, -1.5, 0.3, 2);
            } else {
                frame(stairU, STAIR_W, 2.4, DEPTH * 0.55);
                tanks(stairU, 0, 0.3, 3);
                frame(BL / 2 - ROOM_W, ROOM_W * 1.6, 2.2, DEPTH * 0.6);
                tanks(BL / 2 - ROOM_W, 0, 0.3, 4);
            }
            b.roof = roof;
            b.roofY = 0; b.roofOp = 1;
            roof.position.y = b.floorCount * FH;
            b.group.add(roof);
        }

        // ---------- labels ----------
        function addLabel(node, pos, show, anchor) {
            labelsEl.appendChild(node);
            var l = { el: node, pos: pos, show: show, anchor: anchor, on: false };
            labels.push(l);
            return l;
        }
        function updateLabels() {
            labels.forEach(function (l) {
                l.on = !!l.show();
                l.el.classList.toggle('on', l.on);
                if (l.el.tagName === 'BUTTON') l.el.tabIndex = l.on ? 0 : -1;
                if (l.after) l.after();
            });
        }
        var v3 = new THREE.Vector3();
        function placeLabels() {
            var w = host.clientWidth, h = host.clientHeight;
            for (var i = 0; i < labels.length; i++) {
                var l = labels[i];
                if (!l.on) continue;
                v3.copy(l.pos);
                if (l.el._floor) v3.y += l.el._floor.group.position.y;
                v3.project(camera);
                var x = (v3.x * 0.5 + 0.5) * w, y = (-v3.y * 0.5 + 0.5) * h;
                var t = l.anchor === 'left' ? 'translate(0,-50%)' : l.anchor === 'center' ? 'translate(-50%,-50%)' : 'translate(-50%,-100%)';
                l.el.style.transform = 'translate(' + x.toFixed(1) + 'px,' + y.toFixed(1) + 'px) ' + t;
                l.el.style.visibility = v3.z > 1 ? 'hidden' : '';
            }
        }

        // ---------- colours / visibility targets ----------
        function refreshColors(instant) {
            buildings.forEach(function (b) {
                var active = state.b === b.id;
                b.opTarget = state.b && !active ? 0 : 1; // other buildings fade out so the chosen one is clear
                b.floors.forEach(function (fl) {
                    var above = active && state.f != null && fl.n > state.f;
                    fl.group.userData.yTarget = above ? 6 + (fl.n - state.f) * 2.5 : 0;
                    fl.group.userData.opTarget = above ? 0 : 1;
                });
                b.roofTargetY = active && state.f != null ? 6 + (b.floorCount + 1 - state.f) * 2.5 : 0;
                b.roofTargetOp = active && state.f != null ? 0 : 1;
                b.tag.innerHTML = dhT('ตึก ', 'Building ') + esc(b.id) + '<small>' + countText(roomsOf(b)) + '</small>';
            });
            allRooms.forEach(function (r) {
                // overview shows the real building; the selected building shows each room's status colour
                var active = state.b === r.b.id;
                var c = wallColor(r.b);
                if (active) c = state.onlyBookable && !bookable(r) ? COLORS.neutral : statusColor(r.status);
                r.target.setHex(c);
                if (instant) r.mesh.material.color.copy(r.target);
            });
            updateLabels();
        }
        function roomsOf(b, floor) {
            return allRooms.filter(function (r) { return r.b === b && (floor == null || r.floor === floor); });
        }
        function counts(list) {
            var c = { AVAILABLE: 0, SOON: 0, OCCUPIED: 0, MAINTENANCE: 0, total: list.length };
            list.forEach(function (r) { c[r.status] = (c[r.status] || 0) + 1; });
            return c;
        }
        function countText(list) {
            var c = counts(list);
            return dhT('ว่าง ' + c.AVAILABLE + ' · ใกล้ว่าง ' + c.SOON, c.AVAILABLE + ' free · ' + c.SOON + ' soon');
        }

        // ---------- camera ----------
        var tween = null;
        // camera flight: smooth start and stop, duration grows with the distance (about 0.9 to 1.6 s),
        // and a small lift in the middle so the camera glides over the buildings instead of cutting through
        function flyTo(pos, target, dur) {
            var dist = camera.position.distanceTo(pos) + controls.target.distanceTo(target);
            tween = {
                p0: camera.position.clone(), t0: controls.target.clone(), p1: pos, t1: target, s: performance.now(),
                d: reduceMotion ? 1 : (dur || Math.min(1600, Math.max(900, 700 + dist * 7))),
                lift: Math.min(12, dist * 0.08)
            };
        }
        function narrow() { return root.clientWidth < 760; }
        function siteView() {
            return narrow()
                ? [new THREE.Vector3(-26, 130, 150), new THREE.Vector3(0, 0, 6)]
                : [new THREE.Vector3(-40, 72, 92), new THREE.Vector3(-8, 0, 0)];
        }
        function buildingView(b, floor) {
            var out = b.axis === 'x' ? new THREE.Vector3(0, 0, b.front) : new THREE.Vector3(b.front, 0, 0);
            var side = b.axis === 'x' ? new THREE.Vector3(1, 0, 0) : new THREE.Vector3(0, 0, 1);
            var far = narrow() ? 2 : 1;
            var t, p;
            if (floor == null) {
                t = b.center.clone().setY(b.floorCount * FH / 2);
                // stay above the courtyard so the other buildings never block the view
                p = t.clone().addScaledVector(out, 31 * far).addScaledVector(side, -15 * far).setY(t.y + 16 * far);
            } else {
                t = b.center.clone().setY((floor - 1) * FH + 1.4);
                p = t.clone().addScaledVector(out, 12 * far).addScaledVector(side, -3);
                p.y += (12 + b.L * 0.9) * far;
            }
            return [p, t];
        }

        // ---------- navigation ----------
        function goSite() {
            state.view = 'site'; state.b = null; state.f = null; closeSheet();
            var v = siteView(); flyTo(v[0], v[1]); refreshColors(); renderPanel();
        }
        function goBuilding(id) {
            state.view = 'building'; state.b = id; state.f = null; closeSheet();
            var v = buildingView(find(id)); flyTo(v[0], v[1]); refreshColors(); renderPanel();
        }
        function goFloor(f) {
            state.view = 'floor'; state.f = f;
            if (state.room && state.room.floor !== f) closeSheet();
            var v = buildingView(find(state.b), f); flyTo(v[0], v[1]); refreshColors(); renderPanel();
        }

        // ---------- side panel ----------
        function dot(color) { return '<i class="d3-dot" style="background:' + color + '"></i>'; }
        function legend() {
            return '<div class="d3-legend">' +
                '<span>' + dot(statusCss('AVAILABLE')) + dhT('ว่าง', 'Available') + '</span>' +
                '<span>' + dot(statusCss('SOON')) + dhT('ใกล้หมดสัญญา (ภายใน ' + soonDays + ' วัน)', 'Contract ending (within ' + soonDays + ' days)') + '</span>' +
                '<span>' + dot(statusCss('OCCUPIED')) + dhT('มีผู้เช่า', 'Occupied') + '</span>' +
                '<span>' + dot(statusCss('MAINTENANCE')) + dhT('ปิดซ่อม', 'Maintenance') + '</span></div>';
        }
        function bar(c) {
            var t = c.total || 1;
            return '<span class="d3-bar">' +
                '<i style="width:' + (c.AVAILABLE / t * 100) + '%;background:' + statusCss('AVAILABLE') + '"></i>' +
                '<i style="width:' + (c.SOON / t * 100) + '%;background:' + statusCss('SOON') + '"></i>' +
                '<i style="width:' + (c.OCCUPIED / t * 100) + '%;background:' + statusCss('OCCUPIED') + '"></i>' +
                '<i style="width:' + (c.MAINTENANCE / t * 100) + '%;background:' + statusCss('MAINTENANCE') + '"></i></span>';
        }
        function renderPanel() {
            backBtn.hidden = state.view === 'site' && !state.room;
            root.classList.toggle('d3-has-back', !backBtn.hidden);
            var parts = ['<button type="button" data-go="site">' + dhT('ภาพรวม', 'Overview') + '</button>'];
            if (state.b) parts.push('<span>›</span>', state.f ? '<button type="button" data-go="b">' + dhT('ตึก ', 'Building ') + esc(state.b) + '</button>' : '<span>' + dhT('ตึก ', 'Building ') + esc(state.b) + '</span>');
            if (state.f) parts.push('<span>›</span><span>' + dhT('ชั้น ', 'Floor ') + state.f + '</span>');
            crumbs.innerHTML = parts.join(' ');

            var html;
            if (state.view === 'site') {
                var all = counts(allRooms);
                html = '<div><h2 class="d3-h">' + dhT('เลือกตึก', 'Choose a building') + '</h2><p class="d3-hint">' +
                    dhT('ทั้งหมด ' + all.total + ' ห้อง ว่างตอนนี้ ' + all.AVAILABLE + ' ห้อง และจะว่างภายใน ' + soonDays + ' วันอีก ' + all.SOON + ' ห้อง ลากเพื่อหมุน เลื่อนเพื่อซูม',
                        all.total + ' rooms: ' + all.AVAILABLE + ' free now and ' + all.SOON + ' more within ' + soonDays + ' days. Drag to rotate, scroll to zoom.') +
                    '</p></div><div class="d3-list">';
                buildings.forEach(function (b) {
                    var c = counts(roomsOf(b));
                    html += '<button type="button" class="d3-bcard" data-b="' + esc(b.id) + '"><span class="d3-tag">' + esc(b.id) + '</span>' +
                        '<span><span class="fw-medium">' + dhT('ตึก ', 'Building ') + esc(b.id) + '</span><br><span class="small text-muted">' + dhT(b.floorCount + ' ชั้น · ' + c.total + ' ห้อง', b.floorCount + ' floors · ' + c.total + ' rooms') + '</span></span>' +
                        '<span class="d3-n">' + c.AVAILABLE + dhT(' ว่าง', ' free') + '<span>' + c.SOON + dhT(' ใกล้ว่าง', ' soon') + '</span></span></button>';
                });
                html += '</div>' + legend();
            } else {
                var b = find(state.b);
                html = '<div><h2 class="d3-h">' + dhT('ตึก ', 'Building ') + esc(b.id) + (state.f ? dhT(' ชั้น ', ' · Floor ') + state.f : '') + '</h2><p class="d3-hint">' +
                    (state.f ? (isAdmin ? dhT('กดที่ห้องเพื่อดูรายละเอียด', 'Tap a room for details') : dhT('กดที่ห้องเพื่อดูรายละเอียด ราคา และจอง', 'Tap a room for details, price and booking'))
                        : dhT('สีของแต่ละห้องบอกสถานะ เลือกชั้นเพื่อเปิดดูข้างใน', 'Room colours show their status. Choose a floor to look inside.')) + '</p></div><div class="d3-floors">';
                for (var k = b.roomFloors.length - 1; k >= 0; k--) {
                    var f = b.roomFloors[k];
                    var c2 = counts(roomsOf(b, f));
                    html += '<button type="button" class="d3-fbtn' + (state.f === f ? ' active' : '') + '" data-f="' + f + '"><span class="fw-medium">' + dhT('ชั้น ', 'Floor ') + f + '</span>' + bar(c2) +
                        '<span class="small text-muted">' + dhT('ว่าง ' + c2.AVAILABLE, c2.AVAILABLE + ' free') + '</span></button>';
                }
                html += '</div>';
                if (state.f) {
                    var list = roomsOf(b, state.f).filter(function (r) { return isAdmin || bookable(r); });
                    html += '<div class="d3-sec">' + (isAdmin ? dhT('ห้องในชั้นนี้', 'Rooms on this floor') : dhT('ห้องที่จองได้', 'Rooms you can book')) + '</div>';
                    html += list.length ? '<div class="d3-chips">' + list.map(function (r) {
                        return '<button type="button" class="d3-rbtn" data-r="' + esc(r.number) + '" style="border-color:' + statusCss(r.status) + '">' + esc(r.number) + '<i>฿' + baht(r.rent) + '</i></button>';
                    }).join('') + '</div>' : '<p class="d3-hint">' + dhT('ชั้นนี้ไม่มีห้องว่าง ลองดูชั้นอื่น', 'No free rooms on this floor. Try another floor.') + '</p>';
                }
                html += '<div class="d-flex flex-wrap gap-2"><button type="button" class="btn btn-sm ' + (state.onlyBookable ? 'btn-primary' : 'btn-light') + '" data-toggle="bookable" aria-pressed="' + state.onlyBookable + '">' + dhT('เน้นห้องที่จองได้', 'Highlight bookable rooms') + '</button>' +
                    (state.f ? '<button type="button" class="btn btn-sm btn-light" data-go="b">' + dhT('ดูทั้งตึก', 'Whole building') + '</button>' : '') + '</div>' + legend();
            }
            pbody.innerHTML = html;
        }
        panel.addEventListener('click', function (e) {
            var t = e.target.closest('button');
            if (!t) return;
            if (t.dataset.go === 'site') goSite();
            else if (t.dataset.go === 'b') goBuilding(state.b);
            else if (t.dataset.b) goBuilding(t.dataset.b);
            else if (t.dataset.f) goFloor(+t.dataset.f);
            else if (t.dataset.r) openRoom(byNumber(t.dataset.r));
            else if (t.dataset.toggle) { state.onlyBookable = !state.onlyBookable; refreshColors(); renderPanel(); }
        });
        function byNumber(n) { for (var i = 0; i < allRooms.length; i++) if (allRooms[i].number === n) return allRooms[i]; return null; }

        // ---------- room sheet ----------
        closeBtn.addEventListener('click', function () { closeSheet(); refreshColors(); });
        function closeSheet() { state.room = null; sheet.classList.add('closed'); }

        function openRoom(r, keepCamera) {
            if (!r) return;
            if (state.b !== r.b.id || state.f !== r.floor) {
                state.b = r.b.id; state.f = r.floor; state.view = 'floor';
                if (!keepCamera) { var v = buildingView(r.b, r.floor); flyTo(v[0], v[1]); }
            }
            state.room = r;
            refreshColors(); renderPanel();

            var big = /ใหญ่/.test(r.type || '') || r.capacity >= 3;
            shead.innerHTML = '<h3 class="d3-title">' + dhT('ห้อง ', 'Room ') + esc(r.number) + '</h3><div class="small text-muted">' + dhT('ตึก ', 'Building ') + esc(r.building) + dhT(' · ชั้น ', ' · Floor ') + r.floor +
                ' · ' + (r.side === 0 ? dhT('ฝั่งส่วนกลาง', 'Courtyard side') : dhT('ฝั่งด้านนอก', 'Outer side')) + '</div>';
            var statusLine;
            if (r.status === 'AVAILABLE') statusLine = dhT('ว่าง เข้าอยู่ได้ทันที', 'Available. Move in right away.');
            else if (r.status === 'SOON') statusLine = dhT('สัญญาปัจจุบันหมด <b>' + thDate(r.contractEnd) + '</b> จองล่วงหน้าได้ เข้าอยู่ได้ตั้งแต่ ' + thDate(r.availableFrom),
                'The current contract ends on <b>' + thDate(r.contractEnd) + '</b>. Book ahead to move in from ' + thDate(r.availableFrom) + '.');
            else if (r.status === 'MAINTENANCE') statusLine = dhT('ห้องนี้ปิดซ่อมชั่วคราว', 'This room is closed for maintenance.');
            else statusLine = dhT('มีผู้เช่าแล้ว', 'Occupied') + (r.contractEnd ? dhT(' (สัญญาถึง ', ' (contract until ') + thDate(r.contractEnd) + ')' : '');
            if (r.floor === 1) statusLine += '<br><span class="small text-muted">' + dhT('ห้องชั้น 1 มีประตูเข้าห้องจากด้านนอกตึก', 'Floor 1 rooms have their door on the outside of the building.') + '</span>';

            var html = '<div class="d-flex align-items-center gap-2 flex-wrap"><span class="d3-pill" style="background:' + statusCss(r.status) + '">' + esc(r.statusLabel) + '</span>' +
                (r.pendingBookings ? '<span class="badge soft-warning">' + dhT('มีคำขอจอง ' + r.pendingBookings + ' รายการ', r.pendingBookings + ' booking request' + (r.pendingBookings === 1 ? '' : 's')) + '</span>' : '') + '</div>' +
                '<button type="button" class="d3-preview" data-act="room3d"><span class="d3-preview-ic"><i data-lucide="box"></i></span><span><b>' + dhT('ดูห้องแบบ 3D', 'View room in 3D') + '</b><br><span class="small text-muted">' + dhT('หมุนดูการจัดห้อง ', 'Rotate to see the layout ') + esc(r.type || '') + '</span></span><i data-lucide="chevron-right"></i></button>' +
                '<div class="d3-price"><b>฿' + baht(r.rent) + '</b><span>' + dhT('/ เดือน', '/ month') + '</span></div>' +
                '<div class="d3-status">' + statusLine + '</div>' +
                '<dl class="d3-spec">' +
                '<div><dt>' + dhT('ประเภท', 'Type') + '</dt><dd>' + esc(r.type || '-') + '</dd></div>' +
                '<div><dt>' + dhT('ค่าไฟ / ค่าน้ำ', 'Electricity / water') + '</dt><dd>' + (opts.electricRate || 7) + ' / ' + (opts.waterRate || 20) + dhT(' บาท/หน่วย', ' baht/unit') + '</dd></div>' +
                '<div><dt>' + dhT('รับผู้เช่าได้', 'Capacity') + '</dt><dd>' + dhT(r.capacity + ' คน', r.capacity + (r.capacity === 1 ? ' person' : ' people')) + (r.capacity > 1 ? dhT(' (หารค่าห้องได้)', ' (rent can be shared)') : '') + '</dd></div>' +
                '<div><dt>' + (isAdmin ? dhT('ผู้เช่าตอนนี้', 'Tenants now') : dhT('ต่อคน (ถ้าอยู่เต็ม)', 'Per person (when full)')) + '</dt><dd>' + (isAdmin ? r.occupants + ' / ' + r.capacity + dhT(' คน', '') : '฿' + baht(r.rent / Math.max(1, r.capacity))) + '</dd></div>' +
                '</dl>';

            if (isAdmin) {
                html += '<div><div class="d3-sec mb-1">' + dhT('ผู้เช่า', 'Tenants') + '</div>' + ((r.tenants && r.tenants.length) ? '<ul class="d3-tenants">' + r.tenants.map(function (n) { return '<li>' + esc(n) + '</li>'; }).join('') + '</ul>' : '<div class="small text-muted">' + dhT('ยังไม่มีผู้เช่า', 'No tenants yet') + '</div>') + '</div>' +
                    '<div class="d-flex gap-2 flex-wrap"><a class="btn btn-primary" href="' + esc(opts.editUrl + r.id) + '"><i data-lucide="pencil"></i> ' + dhT('แก้ไขห้อง', 'Edit room') + '</a>' +
                    (r.pendingBookings ? '<a class="btn btn-light" href="' + esc(opts.bookingsUrl) + '"><i data-lucide="inbox"></i> ' + dhT('ดูคำขอจอง', 'View booking requests') + '</a>' : '') + '</div>';
            } else if (bookable(r) && opts.bookTemplate) {
                html += '<div data-slot="book"></div>';
            } else if (bookable(r) && opts.bookBlockedText) {
                html += '<button class="btn btn-light w-100" disabled>' + esc(opts.bookBlockedText) + '</button>';
            } else {
                html += '<button class="btn btn-light w-100" disabled>' + (r.status === 'MAINTENANCE' ? dhT('ปิดซ่อม จองไม่ได้ชั่วคราว', 'Under maintenance, not bookable for now') : dhT('ห้องนี้มีผู้เช่าแล้ว', 'This room is taken')) + '</button>';
            }
            sbody.innerHTML = html;

            var slot = sbody.querySelector('[data-slot="book"]');
            if (slot) {
                var form = opts.bookTemplate.content.firstElementChild.cloneNode(true);
                form.querySelector('[data-field="room"]').value = r.id;
                var date = form.querySelector('[data-field="date"]');
                date.min = r.availableFrom; date.value = r.availableFrom;
                form.querySelector('[data-field="title"]').textContent = (r.status === 'SOON' ? dhT('จองล่วงหน้าห้อง ', 'Book ahead: room ') : dhT('จองห้อง ', 'Book room ')) + r.number;
                slot.appendChild(form);
            }
            if (window.lucide) lucide.createIcons();
            sheet.classList.remove('closed');
            sbody.scrollTop = 0;
        }
        sbody.addEventListener('click', function (e) {
            var t = e.target.closest('[data-act="room3d"]');
            if (t && state.room) openRoom3D(state.room);
        });

        // ---------- picking ----------
        var ray = new THREE.Raycaster(), ndc = new THREE.Vector2(), down = null, hovered = null;
        function pick(e) {
            var rect = renderer.domElement.getBoundingClientRect();
            ndc.set(((e.clientX - rect.left) / rect.width) * 2 - 1, -((e.clientY - rect.top) / rect.height) * 2 + 1);
            ray.setFromCamera(ndc, camera);
            if (state.view === 'site') {
                var hit = ray.intersectObjects(buildings.map(function (b) { return b.group; }), true)[0];
                return hit ? { b: hit.object.userData.building } : null;
            }
            var cand = roomMeshes.filter(function (m) { var r = m.userData.room; return r.b.id === state.b && m.parent.userData.opTarget > 0; });
            var h2 = ray.intersectObjects(cand, false)[0];
            if (h2) return { room: h2.object.userData.room };
            var others = buildings.filter(function (b) { return b.id !== state.b; }).map(function (b) { return b.group; });
            var h3 = ray.intersectObjects(others, true)[0];
            return h3 ? { b: h3.object.userData.building } : null;
        }
        renderer.domElement.addEventListener('pointerdown', function (e) { down = { x: e.clientX, y: e.clientY }; tween = null; });
        renderer.domElement.addEventListener('pointerup', function (e) {
            if (!down) return;
            var moved = Math.hypot(e.clientX - down.x, e.clientY - down.y);
            down = null;
            if (moved > 6) return;
            var p = pick(e);
            if (!p) return;
            if (p.room) openRoom(p.room, state.view === 'floor');
            else if (p.b && p.b !== state.b) goBuilding(p.b);
        });
        renderer.domElement.addEventListener('pointermove', function (e) {
            if (e.pointerType !== 'mouse' || down) { tip.hidden = true; return; }
            var p = pick(e);
            if (hovered) hovered.hover = 0;
            hovered = p && p.room ? p.room : null;
            if (hovered) hovered.hover = 1;
            renderer.domElement.style.cursor = p ? 'pointer' : 'grab';
            if (p && p.room) tip.innerHTML = '<b>' + esc(p.room.number) + '</b> · ' + esc(p.room.statusLabel) + ' · ฿' + baht(p.room.rent);
            else if (p && p.b) tip.innerHTML = dhT('ตึก ', 'Building ') + esc(p.b) + dhT(' · กดเพื่อเข้าไปดู', ' · tap to go in');
            tip.hidden = !p;
            var rect = root.getBoundingClientRect();
            tip.style.transform = 'translate(' + (e.clientX - rect.left + 14) + 'px,' + (e.clientY - rect.top + 14) + 'px)';
        });
        renderer.domElement.addEventListener('pointerleave', function () { tip.hidden = true; if (hovered) hovered.hover = 0; hovered = null; });
        // one step back: room 3D -> room panel -> floor -> building -> overview
        function goBack() {
            if (!roomView.hidden) closeRoom3D();
            else if (state.room) { closeSheet(); refreshColors(); renderPanel(); }
            else if (state.f) goBuilding(state.b);
            else if (state.b) goSite();
        }
        backBtn.addEventListener('click', goBack);
        root.addEventListener('keydown', function (e) { if (e.key === 'Escape') goBack(); });

        // ---------- render loop ----------
        function resize() {
            var w = host.clientWidth, h = host.clientHeight;
            if (!w || !h) return;
            renderer.setSize(w, h, false);
            renderer.domElement.style.width = w + 'px';
            renderer.domElement.style.height = h + 'px';
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
        }
        if (window.ResizeObserver) new ResizeObserver(resize).observe(host); else window.addEventListener('resize', resize);

        var last = performance.now();
        var pulse = 0;
        function loop(now) {
            var dt = Math.min(0.05, (now - last) / 1000);
            last = now;
            if (tween) {
                var k = Math.min(1, (now - tween.s) / tween.d), e = smoother(k);
                camera.position.lerpVectors(tween.p0, tween.p1, e);
                camera.position.y += Math.sin(Math.PI * e) * tween.lift;
                controls.target.lerpVectors(tween.t0, tween.t1, e);
                if (k >= 1) tween = null;
            }
            controls.update();

            var a = reduceMotion ? 1 : damp(7, dt);
            pulse += dt;
            buildings.forEach(function (b) {
                b.op += (b.opTarget - b.op) * a;
                b.floors.forEach(function (fl) {
                    var u = fl.group.userData;
                    u.y += (u.yTarget - u.y) * a;
                    u.op += (u.opTarget - u.op) * a;
                    fl.group.position.y = u.y;
                    var o = u.op * b.op;
                    fl.group.visible = o > 0.02;
                    fl.mats.forEach(function (m) { m.opacity = o * (m.userData.base || 1); });
                    fl.rooms.forEach(function (r) {
                        var m = r.mesh.material;
                        m.color.lerp(r.target, a);
                        m.opacity = o;
                        r.mesh.scale.y += ((r.hover ? 1.1 : 1) - r.mesh.scale.y) * a;
                        if (state.room === r) m.emissive.setHex(COLORS.select).multiplyScalar(0.25 + 0.15 * Math.sin(pulse * 4));
                        else m.emissive.setHex(0x000000);
                    });
                });
                b.roofY += (b.roofTargetY - b.roofY) * a;
                b.roofOp += (b.roofTargetOp * b.op - b.roofOp) * a;
                b.roof.position.y = b.floorCount * FH + b.roofY;
                b.roofMats.forEach(function (m) { m.opacity = b.roofOp; });
                b.roof.visible = b.roofOp > 0.02;
            });

            renderer.render(scene, camera);
            placeLabels();
            requestAnimationFrame(loop);
        }

        function start() {
            // room number tags move with their floor while it lifts away
            buildings.forEach(function (b) {
                b.floors.forEach(function (fl) {
                    fl.rooms.forEach(function (r) { if (r.tag) r.tag._floor = fl; });
                });
            });
            resize();
            var v = siteView();
            camera.position.copy(v[0]).multiplyScalar(1.3);
            camera.position.y += 20;
            controls.target.copy(v[1]);
            renderPanel();
            flyTo(v[0], v[1], 1600);
            requestAnimationFrame(loop);
            if (opts.initialRoom) {
                var r = byNumber(opts.initialRoom);
                if (r) setTimeout(function () { openRoom(r); }, reduceMotion ? 0 : 900);
            }
        }

        // ---------- room 3D (furnished model built from the room type) ----------
        var r3 = null, r3HideTimer = 0;
        function openRoom3D(r) {
            clearTimeout(r3HideTimer);
            if (r3) closeRoom3D(true);
            roomView.innerHTML = '';
            var head = el('div', 'd3-r3-head',
                '<div><div class="fw-semibold">' + dhT('ห้อง ', 'Room ') + esc(r.number) + ' · ' + esc(r.type || '') + '</div>' +
                '<div class="small text-muted">' + dhT('ภาพจำลองจากห้องจริงของหอ (เฟอร์นิเจอร์ตามประเภทห้อง) ลากเพื่อหมุนดู', 'Modelled on the dorm\'s real rooms (furniture depends on room type). Drag to rotate.') + '</div></div>');
            var x = el('button', 'btn btn-light btn-icon', ICON_X);
            x.type = 'button';
            x.setAttribute('aria-label', dhT('ปิดห้อง 3D', 'Close 3D room'));
            x.addEventListener('click', closeRoom3D);
            head.appendChild(x);
            var canvasHost = el('div', 'd3-r3-canvas');
            roomView.appendChild(head);
            roomView.appendChild(canvasHost);
            roomView.hidden = false;
            requestAnimationFrame(function () { roomView.classList.add('on'); });

            var rr = new THREE.WebGLRenderer({ antialias: true, alpha: true });
            rr.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
            rr.shadowMap.enabled = true;
            rr.shadowMap.type = THREE.PCFSoftShadowMap;
            canvasHost.appendChild(rr.domElement);
            var sc = new THREE.Scene();
            var cam = new THREE.PerspectiveCamera(40, 1, 0.1, 100);
            var ctl = new THREE.OrbitControls(cam, rr.domElement);
            ctl.enableDamping = true;
            ctl.dampingFactor = 0.08;
            ctl.enablePan = false;
            ctl.minDistance = 4;
            ctl.maxDistance = 16;
            ctl.maxPolarAngle = 1.25;
            ctl.minAzimuthAngle = -0.35;
            ctl.maxAzimuthAngle = 1.9;

            var model = buildRoomModel(r);
            sc.add(model.group);
            sc.add(new THREE.HemisphereLight(0xffffff, 0xd9cbb5, 0.75));
            var key = new THREE.DirectionalLight(0xfff4e6, 0.7);
            key.position.set(4, 8, 6);
            key.castShadow = true;
            key.shadow.mapSize.set(1024, 1024);
            key.shadow.camera.left = -6; key.shadow.camera.right = 6; key.shadow.camera.top = 6; key.shadow.camera.bottom = -6;
            sc.add(key);
            var lamp = new THREE.PointLight(0xffe2b0, 0.35, 10);
            lamp.position.set(0, 2.4, 0);
            sc.add(lamp);

            var target = new THREE.Vector3(0, 0.9, 0);
            var tall = canvasHost.clientWidth < canvasHost.clientHeight ? 2.1 : 1;
            var endPos = new THREE.Vector3(model.W * 0.8, 4.6, model.D * 0.78).multiplyScalar(tall);
            cam.position.copy(endPos).multiplyScalar(1.8);
            ctl.target.copy(target);
            var t0 = performance.now(), startPos = cam.position.clone();

            function size() {
                var w = canvasHost.clientWidth, h = canvasHost.clientHeight;
                if (!w || !h) return;
                rr.setSize(w, h, false);
                rr.domElement.style.width = w + 'px';
                rr.domElement.style.height = h + 'px';
                cam.aspect = w / h;
                cam.updateProjectionMatrix();
            }
            size();
            var ro = window.ResizeObserver ? new ResizeObserver(size) : null;
            if (ro) ro.observe(canvasHost);

            var lastT = performance.now();
            r3 = { renderer: rr, scene: sc, ro: ro, raf: 0 };
            (function frame(now) {
                var dt = Math.min(0.05, (now - lastT) / 1000);
                lastT = now;
                var k = reduceMotion ? 1 : Math.min(1, (now - t0) / 1300);
                if (k < 1) cam.position.lerpVectors(startPos, endPos, easeInOut(k));
                if (model.fan) model.fan.rotation.y += dt * 5;
                ctl.update();
                rr.render(sc, cam);
                r3.raf = requestAnimationFrame(frame);
            })(performance.now());
            x.focus();
        }
        function closeRoom3D(instant) {
            roomView.classList.remove('on');
            if (r3) {
                cancelAnimationFrame(r3.raf);
                if (r3.ro) r3.ro.disconnect();
                r3.scene.traverse(function (o) {
                    if (o.geometry) o.geometry.dispose();
                    if (o.material) {
                        (Array.isArray(o.material) ? o.material : [o.material]).forEach(function (m) { if (m.map) m.map.dispose(); m.dispose(); });
                    }
                });
                r3.renderer.dispose();
                r3 = null;
            }
            if (instant === true) return;
            r3HideTimer = setTimeout(function () { roomView.hidden = true; roomView.innerHTML = ''; }, reduceMotion ? 0 : 250);
        }

        function plankTexture(tones, seam) {
            var c = document.createElement('canvas');
            c.width = 256; c.height = 256;
            var g = c.getContext('2d');
            for (var row = 0; row < 8; row++) {
                var off = (row % 2) * 64;
                for (var col = -1; col < 3; col++) {
                    g.fillStyle = tones[(row * 3 + col + 4) % tones.length];
                    g.fillRect(col * 128 + off, row * 32, 128, 32);
                    g.strokeStyle = seam;
                    g.strokeRect(col * 128 + off + .5, row * 32 + .5, 128, 32);
                }
            }
            var t = new THREE.CanvasTexture(c);
            t.wrapS = t.wrapT = THREE.RepeatWrapping;
            return t;
        }

        /*
         * Furnished room modelled on the SJD room photos: pale wood-look tiles, white walls with an orange or green
         * accent wall behind the beds, wooden beds with white-panel headboards and black leather mattresses,
         * mirror wardrobe, long desk with two chairs and a TV, black mini fridge with an orange door, split AC
         * above the sliding balcony door with brown curtains, balcony with stainless railing and sink, bathroom.
         * Back wall (z < 0) faces the balcony, front wall (z > 0) has the entrance; front and right walls are cut low.
         */
        function buildRoomModel(r) {
            var big = /ใหญ่/.test(r.type || '') || r.capacity >= 3;
            var hasAir = !/พัดลม/.test(r.type || '');
            var W = big ? 5.0 : 4.2, D = big ? 6.8 : 6.2, H = 2.7, T = 0.12;
            var hash = 0;
            for (var i = 0; i < r.number.length; i++) hash = (hash * 31 + r.number.charCodeAt(i)) % 997;
            var accent = hash % 3 === 0 ? 0x62c23c : 0xe8762c;
            var g = new THREE.Group();
            function box(w, h, d, color, x, y, z, extra) {
                var m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), std(color, extra || {}));
                m.position.set(x, y, z);
                m.castShadow = true;
                m.receiveShadow = true;
                g.add(m);
                return m;
            }
            var WHITE = 0xf5f4f0, WOOD = 0x8d6e55, WOOD_LIGHT = 0xa88a6c, PANEL = 0xf1efe9, MATTRESS = 0x2a2422;
            var x0 = -W / 2, x1 = W / 2, zB = -D / 2, zF = D / 2;

            // floor tiles and walls
            var tex = plankTexture(['#e2d2b6', '#dbcaad', '#e8d9c0', '#decdb2'], 'rgba(140,115,85,.35)');
            tex.repeat.set(W / 2, D / 2);
            var floor = new THREE.Mesh(new THREE.BoxGeometry(W, 0.1, D), std(0xffffff, { map: tex, roughness: 0.55 }));
            floor.position.y = -0.05;
            floor.receiveShadow = true;
            g.add(floor);
            var low = 0.35;
            box(T, H, D, accent, x0 - T / 2, H / 2, 0);                         // accent wall behind the beds
            box(T, low, D, WHITE, x1 + T / 2, low / 2, 0);                      // right wall (cut)
            box(W + T * 2, low, T, WHITE, 0, low / 2, zF + T / 2);              // front wall (cut)
            [[x0, 0, D, 'x'], [0, zB, W, 'z']].forEach(function (s) {           // dark skirting
                if (s[3] === 'x') box(0.02, 0.08, s[2], 0x5b3a28, s[0] + 0.01, 0.04, s[1]);
                else box(s[2], 0.08, 0.02, 0x5b3a28, s[0], 0.04, s[1] + 0.01);
            });

            // back wall with the sliding balcony door, bathroom in the back-right corner
            var bw = 1.5, bd = 1.8;
            var doorX0 = x0 + 0.7, doorX1 = x1 - bw - 0.35;
            box(doorX0 - x0, H, T, WHITE, (x0 + doorX0) / 2, H / 2, zB - T / 2);
            box(x1 - doorX1, H, T, WHITE, (doorX1 + x1) / 2, H / 2, zB - T / 2);
            box(doorX1 - doorX0, H - 2.15, T, WHITE, (doorX0 + doorX1) / 2, 2.15 + (H - 2.15) / 2, zB - T / 2);
            var dw = doorX1 - doorX0;
            [doorX0 + dw * 0.25, doorX0 + dw * 0.75].forEach(function (x) {
                box(dw / 2 - 0.04, 2.1, 0.03, 0xcfe6f2, x, 1.05, zB - T / 2, { transparent: true, opacity: 0.35, roughness: 0.05 });
            });
            [doorX0, doorX0 + dw / 2, doorX1].forEach(function (x) { box(0.06, 2.15, 0.08, 0xf4f4f4, x, 1.075, zB - T / 2); });
            box(dw, 0.06, 0.08, 0xf4f4f4, (doorX0 + doorX1) / 2, 2.15, zB - T / 2);
            box(dw + 0.5, 0.03, 0.03, 0xd0d0d0, (doorX0 + doorX1) / 2, 2.35, zB + 0.12);   // curtain rod
            for (var k = 0; k < 6; k++) {                                                 // brown curtain, pulled to the left
                box(0.13, 2.2, 0.08, k % 2 ? 0x7b6a5a : 0x857463, doorX0 - 0.25 + k * 0.12, 1.22, zB + 0.12 + (k % 2) * 0.04);
            }
            if (hasAir) {
                box(0.95, 0.3, 0.22, 0xfbfbfb, (doorX0 + doorX1) / 2 + 0.2, 2.47, zB + 0.11);
                box(0.85, 0.03, 0.02, 0xd5dbe3, (doorX0 + doorX1) / 2 + 0.2, 2.35, zB + 0.23);
            }

            // balcony: wood-look floor, stainless railing, small sink on a green counter (floor 1 rooms open to the outside instead)
            if (r.floor !== 1) {
            var balTex = plankTexture(['#b98d62', '#b0845a', '#c0956b', '#a97d55'], 'rgba(80,50,30,.3)');
            balTex.repeat.set(W / 2, 0.6);
            var bal = new THREE.Mesh(new THREE.BoxGeometry(W, 0.1, 1.25), std(0xffffff, { map: balTex, roughness: 0.7 }));
            bal.position.set(0, -0.08, zB - T - 0.62);
            bal.receiveShadow = true;
            g.add(bal);
            box(T, H, 1.25, WHITE, x0 - T / 2, H / 2, zB - T - 0.62);
            [0.3, 0.55, 0.8, 1.05].forEach(function (h) { box(W, 0.035, 0.035, 0xc9cfd6, 0, h, zB - T - 1.2, { metalness: 0.7, roughness: 0.25 }); });
            [x0 + 0.05, 0, x1 - 0.05].forEach(function (x) { box(0.05, 1.1, 0.05, 0xc9cfd6, x, 0.55, zB - T - 1.2, { metalness: 0.7, roughness: 0.25 }); });
            box(0.5, 0.08, 0.7, 0x46c23c, x1 - 0.3, 0.85, zB - T - 0.62);
            box(0.36, 0.1, 0.45, 0xd9dde1, x1 - 0.3, 0.87, zB - T - 0.62, { metalness: 0.6, roughness: 0.3 });
            box(0.08, 0.85, 0.08, 0xdddddd, x1 - 0.3, 0.42, zB - T - 0.62);
            }

            // bathroom (back-right corner) with dark door frame and toilet
            var bx0 = x1 - bw, bz1 = zB + bd;
            var tile = new THREE.Mesh(new THREE.BoxGeometry(bw, 0.02, bd), std(0xe9eef1));
            tile.position.set(bx0 + bw / 2, 0.01, zB + bd / 2);
            g.add(tile);
            box(0.1, H, bd, WHITE, bx0, H / 2, zB + bd / 2);
            box(bw - 0.8, H, 0.1, WHITE, bx0 + 0.8 + (bw - 0.8) / 2, H / 2, bz1);
            box(0.8, H - 2.05, 0.1, WHITE, bx0 + 0.4, 2.05 + (H - 2.05) / 2, bz1);
            [bx0 + 0.07, bx0 + 0.78].forEach(function (x) { box(0.06, 2.05, 0.14, 0x2e2e30, x, 1.025, bz1); });
            box(0.77, 0.06, 0.14, 0x2e2e30, bx0 + 0.42, 2.05, bz1);
            var door = box(0.04, 2.0, 0.68, 0xf2f2f2, bx0 + 0.12, 1.0, bz1 + 0.36);
            door.rotation.y = 0.25;
            box(0.42, 0.42, 0.55, 0xffffff, x1 - 0.35, 0.21, zB + 0.45);
            box(0.42, 0.38, 0.2, 0xffffff, x1 - 0.35, 0.62, zB + 0.12);
            box(bw - 0.12, 1.4, 0.02, 0x8a7b6e, bx0 + bw / 2, 1.3, zB + 0.02);       // tile accent strip

            // beds against the accent wall: double bed for 1 person, single beds for shared rooms
            function nightstand(z) {
                box(0.42, 0.5, 0.45, WOOD, x0 + 0.24, 0.25, z);
                box(0.02, 0.16, 0.36, 0xf3f3f0, x0 + 0.46, 0.38, z);
            }
            function bed(zc, width) {
                var len = 2.0;
                box(len + 0.08, 0.28, width + 0.08, WOOD_LIGHT, x0 + 0.06 + len / 2, 0.2, zc);
                box(len, 0.22, width, MATTRESS, x0 + 0.08 + len / 2, 0.45, zc, { roughness: 0.45 });
                box(0.12, 1.05, width + 0.12, WOOD, x0 + 0.06, 0.52, zc);
                var n = 3, pw = (width - 0.3) / n;
                for (var p = 0; p < n; p++) box(0.02, 0.38, pw - 0.06, PANEL, x0 + 0.13, 0.78, zc - (width - 0.3) / 2 + pw * (p + 0.5));
            }
            var people = Math.max(1, Math.min(3, r.capacity || 1));
            var zStart = zB + 1.0;
            if (people === 1) {
                bed(zStart + 0.9, 1.7);
                nightstand(zStart + 2.05);
            } else {
                for (var b2 = 0; b2 < people; b2++) {
                    var zc = zStart + 0.55 + b2 * 1.5;
                    bed(zc, 1.05);
                    if (b2 < people - 1) nightstand(zc + 0.75);
                }
            }

            // wardrobe with mirror door (front-left corner)
            var wx = x0 + 0.75, wz = zF - 0.33;
            box(1.3, 2.05, 0.6, WOOD, wx, 1.025, wz);
            box(0.42, 1.95, 0.02, 0x8b6b52, wx - 0.43, 1.03, wz - 0.31);
            box(0.42, 1.95, 0.02, 0xf4f3ef, wx, 1.03, wz - 0.31);
            box(0.42, 1.95, 0.02, 0xc9d3da, wx + 0.43, 1.03, wz - 0.31, { metalness: 0.7, roughness: 0.1 });
            box(0.02, 0.25, 0.02, 0x9aa0a6, wx - 0.05, 1.05, wz - 0.33);
            box(0.02, 0.25, 0.02, 0x9aa0a6, wx + 0.05, 1.05, wz - 0.33);

            // entrance door (cream panel door) on the front wall
            var ex = x1 - 1.0;
            box(0.95, 2.1, 0.06, 0xe9dfc3, ex, 1.05, zF - 0.02);
            box(1.05, 0.06, 0.1, 0x2e2e30, ex, 2.13, zF);
            [ex - 0.5, ex + 0.5].forEach(function (x) { box(0.05, 2.13, 0.1, 0x2e2e30, x, 1.065, zF); });
            box(0.36, 0.26, 0.08, 0xeeeeea, ex - 0.9, 2.3, zF - 0.02);       // electrical box

            // black mini fridge with orange door, next to the bathroom
            var fz = bz1 + 0.4;
            box(0.5, 0.85, 0.5, 0x1f1f21, x1 - 0.3, 0.425, fz);
            box(0.03, 0.72, 0.42, 0xe8702a, x1 - 0.56, 0.43, fz);

            // long desk along the right wall: white drawers, open shelf, two chairs, TV and Wi-Fi router
            var dz0 = fz + 0.4, dLen = Math.min(2.2, zF - 1.4 - dz0), dzc = dz0 + dLen / 2, dx = x1 - 0.32;
            if (dLen > 1.2) {
                box(0.62, 0.05, dLen, WOOD, dx, 0.75, dzc);
                box(0.02, 0.14, 0.5, 0xf3f3f0, dx - 0.31, 0.65, dz0 + 0.3);
                box(0.02, 0.14, 0.5, 0xf3f3f0, dx - 0.31, 0.65, dz0 + dLen - 0.3);
                box(0.55, 0.72, 0.04, WOOD, dx, 0.36, dz0 + 0.02);
                box(0.55, 0.72, 0.04, WOOD, dx, 0.36, dz0 + dLen - 0.02);
                box(0.55, 0.72, 0.5, 0x7d604a, dx + 0.02, 0.36, dzc);
                [0.25, 0.5].forEach(function (h) { box(0.5, 0.025, 0.46, 0xa58468, dx - 0.02, h, dzc); });
                box(0.06, 0.45, 0.8, 0x18191b, x1 - 0.2, 1.05, dzc, { roughness: 0.3 });
                box(0.2, 0.04, 0.3, 0x18191b, x1 - 0.2, 0.79, dzc);
                box(0.15, 0.04, 0.22, 0xfafafa, dx, 0.8, dz0 + 0.25);
                [dz0 + 0.45, dz0 + dLen - 0.45].forEach(function (cz) {
                    if (cz < dzc - 0.3 || cz > dzc + 0.3) {
                        box(0.45, 0.05, 0.42, 0xc8beb0, dx - 0.65, 0.45, cz);
                        box(0.05, 0.5, 0.42, 0xc8beb0, dx - 0.9, 0.72, cz);
                        [-0.18, 0.18].forEach(function (sx) { [-0.16, 0.16].forEach(function (sz) { box(0.04, 0.43, 0.04, 0xbdb3a5, dx - 0.65 + sx, 0.215, cz + sz); }); });
                    }
                });
            }

            // ceiling fan instead of AC in fan rooms
            var fan = null;
            if (!hasAir) {
                fan = new THREE.Group();
                fan.add(new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.1, 0.12, 16), std(0xffffff)));
                for (var bl = 0; bl < 3; bl++) {
                    var blade = new THREE.Mesh(new THREE.BoxGeometry(0.75, 0.02, 0.14), std(0xf3f3f3));
                    blade.position.x = 0.42;
                    var arm = new THREE.Group();
                    arm.rotation.y = bl * Math.PI * 2 / 3;
                    arm.add(blade);
                    fan.add(arm);
                }
                fan.position.set(0, H - 0.25, 0.3);
                g.add(fan);
            }
            g.position.z = 0.6; // keep the balcony inside the view
            return { group: g, W: W, D: D + 1.2, fan: fan };
        }
    }

    window.Dorm3D = { mount: mount };
})();
