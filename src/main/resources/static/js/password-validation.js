(function () {
    function evaluatePassword(value) {
        return {
            length: value.length >= 8,
            uppercase: /[A-Z]/.test(value),
            digit: /[0-9]/.test(value),
            special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(value)
        };
    }

    function attach(passwordInputId, rulesListId) {
        var input = document.getElementById(passwordInputId);
        var rulesList = document.getElementById(rulesListId);
        if (!input || !rulesList) {
            return;
        }

        input.addEventListener("input", function () {
            var result = evaluatePassword(input.value);
            Object.keys(result).forEach(function (rule) {
                var li = rulesList.querySelector('[data-rule="' + rule + '"]');
                if (!li) {
                    return;
                }
                li.classList.toggle("valid", result[rule]);
            });
        });
    }

    function attachMatchCheck(passwordInputId, confirmInputId, messageId) {
        var password = document.getElementById(passwordInputId);
        var confirm = document.getElementById(confirmInputId);
        var message = document.getElementById(messageId);
        if (!password || !confirm || !message) {
            return;
        }

        function check() {
            if (confirm.value.length === 0) {
                message.textContent = "";
                return;
            }
            message.textContent = confirm.value === password.value ? "" : "Şifreler eşleşmiyor";
        }

        password.addEventListener("input", check);
        confirm.addEventListener("input", check);
    }

    window.TaxiHubPasswordValidation = {
        attach: attach,
        attachMatchCheck: attachMatchCheck
    };
})();
