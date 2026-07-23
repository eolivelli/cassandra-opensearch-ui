/*
 * Copyright 2026 Enrico Olivelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbui.store.order;

import jakarta.validation.constraints.NotBlank;

/**
 * The (mock) checkout form. The card details are never stored - only the last four digits are kept
 * on the order. No real payment is processed; the payment always "succeeds".
 *
 * @param shipTo
 *            shipping address
 * @param cardName
 *            name on the card
 * @param cardNumber
 *            card number (only the last 4 digits are retained)
 * @param expiry
 *            card expiry, MM/YY
 * @param cvv
 *            card verification value (discarded immediately)
 */
public record CheckoutRequest(@NotBlank String shipTo, @NotBlank String cardName,
        @NotBlank String cardNumber, @NotBlank String expiry, @NotBlank String cvv) {
}
