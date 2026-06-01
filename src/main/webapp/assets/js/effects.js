/* StoreFX — 사이트 전역 Three.js + GSAP 인터랙션 모듈
   defer 로딩 + DOMContentLoaded 이후 init 함수만 노출.
   모든 함수는 prefers-reduced-motion 존중. */
(function () {
    'use strict';

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const isMobile = window.matchMedia('(max-width: 768px)').matches;

    function whenReady(fn) {
        if (document.readyState !== 'loading') fn();
        else document.addEventListener('DOMContentLoaded', fn);
    }

    // ───────────────────────────────────────────────────────────────────
    // 1. Particle field (Three.js Points)
    // ───────────────────────────────────────────────────────────────────
    function particleField(canvas, opts = {}) {
        if (reducedMotion || !window.THREE) return null;

        const count = opts.count || (isMobile ? 500 : 1500);
        const range = opts.range || 60;
        const colorA = new THREE.Color(opts.colorA || '#8b5cf6');
        const colorB = new THREE.Color(opts.colorB || '#06d6a0');

        const scene = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(75, canvas.clientWidth / canvas.clientHeight, 0.1, 200);
        camera.position.z = 40;

        const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        renderer.setSize(canvas.clientWidth, canvas.clientHeight, false);

        const positions = new Float32Array(count * 3);
        const colors = new Float32Array(count * 3);
        for (let i = 0; i < count; i++) {
            positions[i * 3] = (Math.random() - 0.5) * range * 2;
            positions[i * 3 + 1] = (Math.random() - 0.5) * range * 2;
            positions[i * 3 + 2] = (Math.random() - 0.5) * range * 2;
            const mix = Math.random();
            const c = new THREE.Color().lerpColors(colorA, colorB, mix);
            colors[i * 3] = c.r; colors[i * 3 + 1] = c.g; colors[i * 3 + 2] = c.b;
        }
        const geom = new THREE.BufferGeometry();
        geom.setAttribute('position', new THREE.BufferAttribute(positions, 3));
        geom.setAttribute('color', new THREE.BufferAttribute(colors, 3));
        const mat = new THREE.PointsMaterial({
            size: 0.4, vertexColors: true, transparent: true,
            opacity: 0.8, depthWrite: false,
            blending: THREE.AdditiveBlending
        });
        const points = new THREE.Points(geom, mat);
        scene.add(points);

        const mouse = { x: 0, y: 0, tx: 0, ty: 0 };
        const onMove = (e) => {
            const rect = canvas.getBoundingClientRect();
            mouse.tx = ((e.clientX - rect.left) / rect.width - 0.5) * 2;
            mouse.ty = ((e.clientY - rect.top) / rect.height - 0.5) * 2;
        };
        canvas.addEventListener('mousemove', onMove);

        let rafId, lastT = performance.now();
        function frame(t) {
            if (document.hidden) { rafId = requestAnimationFrame(frame); return; }
            const dt = Math.min((t - lastT) / 1000, 0.05); lastT = t;
            mouse.x += (mouse.tx - mouse.x) * 0.05;
            mouse.y += (mouse.ty - mouse.y) * 0.05;
            points.rotation.y += 0.0005 + mouse.x * 0.002;
            points.rotation.x += -0.0002 + mouse.y * 0.002;
            renderer.render(scene, camera);
            rafId = requestAnimationFrame(frame);
        }
        rafId = requestAnimationFrame(frame);

        const onResize = () => {
            const w = canvas.clientWidth, h = canvas.clientHeight;
            camera.aspect = w / h; camera.updateProjectionMatrix();
            renderer.setSize(w, h, false);
        };
        window.addEventListener('resize', onResize);

        // unload 정리
        const dispose = () => {
            cancelAnimationFrame(rafId);
            window.removeEventListener('resize', onResize);
            canvas.removeEventListener('mousemove', onMove);
            geom.dispose(); mat.dispose(); renderer.dispose();
        };
        window.addEventListener('beforeunload', dispose);
        return { dispose };
    }

    // ───────────────────────────────────────────────────────────────────
    // 2. Orbital wireframe rings (hero 배경)
    // ───────────────────────────────────────────────────────────────────
    function orbitalRings(canvas) {
        if (reducedMotion || !window.THREE) return null;

        const scene = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(60, canvas.clientWidth / canvas.clientHeight, 0.1, 100);
        camera.position.z = 18;

        const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        renderer.setSize(canvas.clientWidth, canvas.clientHeight, false);

        const rings = [];
        const configs = [
            { r: 5,  t: 0.04, color: 0x8b5cf6, sx: 1.0, sy: 0.3, sz: 0.0, speed: 0.004 },
            { r: 7,  t: 0.03, color: 0x06d6a0, sx: 0.0, sy: 1.0, sz: 0.4, speed: 0.003 },
            { r: 9,  t: 0.02, color: 0xb583ff, sx: 0.5, sy: 0.5, sz: 1.0, speed: 0.002 },
        ];
        configs.forEach(c => {
            const geom = new THREE.TorusGeometry(c.r, c.t, 8, 100);
            const mat = new THREE.MeshBasicMaterial({ color: c.color, wireframe: true, transparent: true, opacity: 0.55 });
            const mesh = new THREE.Mesh(geom, mat);
            scene.add(mesh);
            rings.push({ mesh, ...c });
        });

        let rafId;
        function frame() {
            if (document.hidden) { rafId = requestAnimationFrame(frame); return; }
            rings.forEach(r => {
                r.mesh.rotation.x += r.speed * r.sx;
                r.mesh.rotation.y += r.speed * r.sy;
                r.mesh.rotation.z += r.speed * r.sz;
            });
            renderer.render(scene, camera);
            rafId = requestAnimationFrame(frame);
        }
        rafId = requestAnimationFrame(frame);

        const onResize = () => {
            const w = canvas.clientWidth, h = canvas.clientHeight;
            camera.aspect = w / h; camera.updateProjectionMatrix();
            renderer.setSize(w, h, false);
        };
        window.addEventListener('resize', onResize);

        const dispose = () => {
            cancelAnimationFrame(rafId);
            window.removeEventListener('resize', onResize);
            rings.forEach(r => { r.mesh.geometry.dispose(); r.mesh.material.dispose(); });
            renderer.dispose();
        };
        window.addEventListener('beforeunload', dispose);
        return { dispose };
    }

    // ───────────────────────────────────────────────────────────────────
    // 3. Reveal on scroll
    // ───────────────────────────────────────────────────────────────────
    function revealOnScroll(root = document) {
        if (reducedMotion) {
            root.querySelectorAll('[data-reveal]').forEach(el => el.classList.add('is-visible'));
            return;
        }
        const targets = root.querySelectorAll('[data-reveal]');
        if (!targets.length) return;

        const io = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) return;
                const el = entry.target;
                const delay = parseFloat(el.dataset.revealDelay || '0');
                if (window.gsap) {
                    gsap.fromTo(el,
                        { y: 30, opacity: 0 },
                        { y: 0, opacity: 1, duration: 0.7, delay, ease: 'power3.out',
                          onStart: () => el.classList.add('is-visible') });
                } else {
                    el.classList.add('is-visible');
                }
                io.unobserve(el);
            });
        }, { threshold: 0.15, rootMargin: '0px 0px -10% 0px' });

        targets.forEach(t => io.observe(t));
    }

    // ───────────────────────────────────────────────────────────────────
    // 4. Magnetic button
    // ───────────────────────────────────────────────────────────────────
    function magnetic(el, strength = 0.25) {
        if (reducedMotion || isMobile) return;
        const onMove = (e) => {
            const rect = el.getBoundingClientRect();
            const x = e.clientX - rect.left - rect.width / 2;
            const y = e.clientY - rect.top - rect.height / 2;
            if (window.gsap) {
                gsap.to(el, { x: x * strength, y: y * strength, duration: 0.3, ease: 'power2.out' });
            } else {
                el.style.transform = `translate(${x*strength}px, ${y*strength}px)`;
            }
        };
        const onLeave = () => {
            if (window.gsap) {
                gsap.to(el, { x: 0, y: 0, duration: 0.6, ease: 'elastic.out(1, 0.4)' });
            } else {
                el.style.transform = '';
            }
        };
        el.addEventListener('mousemove', onMove);
        el.addEventListener('mouseleave', onLeave);
    }

    // ───────────────────────────────────────────────────────────────────
    // 5. Text reveal split (헤드라인용)
    // ───────────────────────────────────────────────────────────────────
    function textRevealSplit(el) {
        if (!el) return;
        const text = el.textContent;
        el.textContent = '';
        const chars = [];
        text.split('').forEach(ch => {
            const span = document.createElement('span');
            span.textContent = ch === ' ' ? ' ' : ch;
            span.style.display = 'inline-block';
            span.style.willChange = 'transform, opacity';
            el.appendChild(span);
            chars.push(span);
        });
        if (reducedMotion || !window.gsap) {
            chars.forEach(s => { s.style.opacity = 1; });
            return;
        }
        gsap.from(chars, {
            y: 40, opacity: 0, duration: 0.7,
            stagger: 0.025, ease: 'power3.out'
        });
    }

    // ───────────────────────────────────────────────────────────────────
    // 자동 적용
    // ───────────────────────────────────────────────────────────────────
    whenReady(() => {
        // 모든 페이지에서 reveal 자동 적용
        revealOnScroll();
        // [data-magnetic] 자동 적용
        document.querySelectorAll('[data-magnetic]').forEach(el => magnetic(el));
        // [data-text-split] 자동 적용
        document.querySelectorAll('[data-text-split]').forEach(el => textRevealSplit(el));
    });

    // 외부 노출
    window.StoreFX = { particleField, orbitalRings, revealOnScroll, magnetic, textRevealSplit };
})();
