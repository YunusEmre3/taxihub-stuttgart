(function () {
    // ---------- Search + account type filter over the customer table ----------
    var searchBox = document.getElementById("customerSearchBox");
    var typeFilter = document.getElementById("accountTypeFilter");
    var rows = document.querySelectorAll(".data-table tbody tr");

    function applyFilters() {
        var query = searchBox ? searchBox.value.trim().toLowerCase() : "";
        var type = typeFilter ? typeFilter.value : "ALL";

        rows.forEach(function (row) {
            var matchesType = type === "ALL" || row.getAttribute("data-account-type") === type;
            var matchesSearch = query === "" || (row.getAttribute("data-search") || "").includes(query);
            row.classList.toggle("is-hidden", !(matchesType && matchesSearch));
        });
    }

    if (searchBox) {
        searchBox.addEventListener("input", applyFilters);
    }
    if (typeFilter) {
        typeFilter.addEventListener("change", applyFilters);
    }

    // ---------- "Add New Customer" modal ----------
    var addModal = document.getElementById("addCustomerModal");
    var openBtn = document.getElementById("openAddCustomerBtn");
    var closeBtn = document.getElementById("addCustomerCloseBtn");

    if (openBtn && addModal) {
        openBtn.addEventListener("click", function () {
            addModal.classList.add("open");
        });
    }
    if (closeBtn && addModal) {
        closeBtn.addEventListener("click", function () {
            addModal.classList.remove("open");
        });
    }
    if (addModal) {
        addModal.addEventListener("click", function (event) {
            if (event.target === addModal) {
                addModal.classList.remove("open");
            }
        });
    }
})();
