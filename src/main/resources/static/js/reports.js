(function () {
    var dateFromInput = document.getElementById("dateFrom");
    var dateToInput = document.getElementById("dateTo");
    var vehicleTypeSelect = document.getElementById("vehicleTypeFilter");
    var exportBtn = document.getElementById("exportDataBtn");

    var avgPassengersEl = document.getElementById("avgPassengersStat");
    var completionRateEl = document.getElementById("completionRateStat");
    var activeDriversEl = document.getElementById("activeDriversStat");

    var CHART_YELLOW = "#F5A623";
    var CHART_GREEN = "#2E7D32";
    var CHART_BLUE = "#1565C0";
    var CHART_RED = "#D32F2F";
    var PIE_COLORS = [CHART_YELLOW, CHART_GREEN, CHART_RED, "#6B6B6B"];

    var volumeChart = null;
    var hourlyChart = null;
    var vehicleTypeChart = null;
    var driverChart = null;

    function buildQuery() {
        var params = new URLSearchParams();
        if (dateFromInput.value) {
            params.set("dateFrom", dateFromInput.value);
        }
        if (dateToInput.value) {
            params.set("dateTo", dateToInput.value);
        }
        if (vehicleTypeSelect.value) {
            params.set("vehicleType", vehicleTypeSelect.value);
        }
        return params.toString();
    }

    function destroyIfExists(chart) {
        if (chart) {
            chart.destroy();
        }
    }

    function renderCharts(data) {
        avgPassengersEl.textContent = data.avgPassengersPerRide.toFixed(1);
        completionRateEl.textContent = data.completionRatePercent.toFixed(0) + "%";
        activeDriversEl.textContent = data.activeDriverCount;

        destroyIfExists(volumeChart);
        volumeChart = new Chart(document.getElementById("volumeChart"), {
            type: "line",
            data: {
                labels: data.monthlyLabels,
                datasets: [{
                    label: "Bookings",
                    data: data.monthlyBookingVolume,
                    borderColor: CHART_YELLOW,
                    backgroundColor: "rgba(245, 166, 35, 0.15)",
                    fill: true,
                    tension: 0.35,
                    pointBackgroundColor: CHART_YELLOW
                }]
            },
            options: {
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
            }
        });

        destroyIfExists(hourlyChart);
        hourlyChart = new Chart(document.getElementById("hourlyChart"), {
            type: "bar",
            data: {
                labels: data.hourLabels,
                datasets: [{
                    label: "Rides",
                    data: data.hourlyRideVolume,
                    backgroundColor: CHART_YELLOW,
                    borderRadius: 4
                }]
            },
            options: {
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
            }
        });

        destroyIfExists(vehicleTypeChart);
        vehicleTypeChart = new Chart(document.getElementById("vehicleTypeChart"), {
            type: "pie",
            data: {
                labels: data.vehicleTypeLabels,
                datasets: [{
                    data: data.vehicleTypeCounts,
                    backgroundColor: PIE_COLORS
                }]
            },
            options: {
                plugins: { legend: { position: "bottom" } }
            }
        });

        destroyIfExists(driverChart);
        var driverChartCanvas = document.getElementById("driverChart");
        var driverChartEmpty = document.getElementById("driverChartEmpty");
        if (data.topDriverNames.length === 0) {
            driverChartCanvas.style.display = "none";
            driverChartEmpty.style.display = "block";
        } else {
            driverChartCanvas.style.display = "block";
            driverChartEmpty.style.display = "none";
            driverChart = new Chart(driverChartCanvas, {
                type: "bar",
                data: {
                    labels: data.topDriverNames,
                    datasets: [{
                        label: "Completed Rides",
                        data: data.topDriverCompletedRides,
                        backgroundColor: CHART_BLUE,
                        borderRadius: 4
                    }]
                },
                options: {
                    indexAxis: "y",
                    plugins: { legend: { display: false } },
                    scales: { x: { beginAtZero: true, ticks: { precision: 0 } } }
                }
            });
        }
    }

    function loadData() {
        fetch("/api/reports/data?" + buildQuery())
            .then(function (response) { return response.json(); })
            .then(renderCharts)
            .catch(function () {
                avgPassengersEl.textContent = "-";
                completionRateEl.textContent = "-";
                activeDriversEl.textContent = "-";
            });
    }

    [dateFromInput, dateToInput, vehicleTypeSelect].forEach(function (el) {
        el.addEventListener("change", loadData);
    });

    if (exportBtn) {
        exportBtn.addEventListener("click", function () {
            window.location.href = "/reports/export?" + buildQuery();
        });
    }

    loadData();
})();
