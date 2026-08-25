(function () {
    var STUTTGART_CENTER = [48.7758, 9.1829];

    var map = L.map("routeMap").setView(STUTTGART_CENTER, 12);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors",
        maxZoom: 19
    }).addTo(map);

    var routeLayer = null;
    var pickupMarker = null;
    var dropoffMarker = null;

    var pickupInput = document.getElementById("pickupAddress");
    var dropoffInput = document.getElementById("dropoffAddress");
    var vehicleTypeSelect = document.getElementById("vehicleType");
    var showRouteBtn = document.getElementById("showRouteBtn");
    var routeStatus = document.getElementById("routeStatus");
    var priceInput = document.getElementById("estimatedPrice");
    var distanceStat = document.getElementById("distanceStat");
    var durationStat = document.getElementById("durationStat");
    var routeStats = document.getElementById("routeStats");
    var pickupLatInput = document.getElementById("pickupLat");
    var pickupLngInput = document.getElementById("pickupLng");

    var csrfToken = document.querySelector('meta[name="_csrf"]');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]');

    function setStatus(message, kind) {
        if (!message) {
            routeStatus.classList.remove("visible", "route-status--error", "route-status--loading");
            return;
        }
        routeStatus.textContent = message;
        routeStatus.className = "route-status visible route-status--" + kind;
    }

    function clearRoute() {
        if (routeLayer) {
            map.removeLayer(routeLayer);
            routeLayer = null;
        }
        if (pickupMarker) {
            map.removeLayer(pickupMarker);
            pickupMarker = null;
        }
        if (dropoffMarker) {
            map.removeLayer(dropoffMarker);
            dropoffMarker = null;
        }
        routeStats.style.display = "none";
        priceInput.value = "";
        pickupLatInput.value = "";
        pickupLngInput.value = "";
    }

    function drawRoute(result) {
        clearRoute();

        var latLngs = result.path.map(function (point) {
            return [point.lat, point.lon];
        });

        routeLayer = L.polyline(latLngs, { color: "#1565C0", weight: 5 }).addTo(map);
        pickupMarker = L.marker([result.pickup.lat, result.pickup.lon]).addTo(map).bindPopup("Alış Noktası");
        dropoffMarker = L.marker([result.dropoff.lat, result.dropoff.lon]).addTo(map).bindPopup("Varış Noktası");

        map.fitBounds(routeLayer.getBounds(), { padding: [24, 24] });

        distanceStat.textContent = result.distanceKm.toFixed(1) + " km";
        durationStat.textContent = Math.round(result.durationMinutes) + " dk";
        routeStats.style.display = "grid";
        priceInput.value = result.estimatedPrice.toFixed(2) + " €";

        // Feeds the dispatch map later - see BookingCreateForm.pickupLat/pickupLng.
        pickupLatInput.value = result.pickup.lat;
        pickupLngInput.value = result.pickup.lon;
    }

    function showRoute() {
        var pickupAddress = pickupInput.value.trim();
        var dropoffAddress = dropoffInput.value.trim();

        if (!pickupAddress || !dropoffAddress) {
            return;
        }

        setStatus("Rota hesaplanıyor...", "loading");
        clearRoute();

        var headers = { "Content-Type": "application/json" };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader.content] = csrfToken.content;
        }

        fetch("/api/bookings/calculate-route", {
            method: "POST",
            headers: headers,
            body: JSON.stringify({
                pickupAddress: pickupAddress,
                dropoffAddress: dropoffAddress,
                vehicleType: vehicleTypeSelect.value || null
            })
        })
            .then(function (response) {
                return response.json();
            })
            .then(function (result) {
                if (!result.success) {
                    setStatus(result.errorMessage || "Rota hesaplanamadı, lütfen adresleri kontrol edin.", "error");
                    return;
                }
                setStatus(null);
                drawRoute(result);
            })
            .catch(function () {
                setStatus("Rota hesaplanamadı, lütfen adresleri kontrol edin.", "error");
            });
    }

    // Trigger automatically once both fields are filled in, and always offer
    // the button for an explicit (re)try. Vehicle type changes the price
    // tier, so re-quote whenever it changes too (if a route is already shown).
    pickupInput.addEventListener("blur", showRoute);
    dropoffInput.addEventListener("blur", showRoute);
    vehicleTypeSelect.addEventListener("change", showRoute);
    showRouteBtn.addEventListener("click", showRoute);
})();
