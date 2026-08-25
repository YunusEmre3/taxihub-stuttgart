(function () {
    var mapEl = document.getElementById("serviceAreaMap");
    if (!mapEl) {
        return;
    }

    // Purely informational - there's no geofencing/region-boundary data
    // anywhere in the system, so this just centers on Stuttgart rather than
    // claiming to show a real service-area polygon.
    var map = L.map("serviceAreaMap", { zoomControl: false, dragging: false, scrollWheelZoom: false })
        .setView([48.7758, 9.1829], 11);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors",
        maxZoom: 19
    }).addTo(map);
})();
