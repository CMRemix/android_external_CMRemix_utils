/*
 * Copyright (C) 2016 Emotroid Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.utils.cmremix;

import cyanogenmod.providers.WeatherContract;

import static cyanogenmod.providers.WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.WeatherCode.SCATTERED_THUNDERSTORMS;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.WeatherCode.SCATTERED_SNOW_SHOWERS;
import static cyanogenmod.providers.WeatherContract.WeatherColumns.WeatherCode.ISOLATED_THUNDERSHOWERS;

public final class WeatherControllerUtils {

	/**
     * Adds an offset to the condition code reported by the active weather service provider.
     * @param conditionCode The condition code from the Weather API
     * @return A condition code that correctly maps to our resource IDs
     */
    public static int addOffsetToConditionCodeFromWeatherContract(int conditionCode) {
        if (conditionCode <= WeatherContract.WeatherColumns.WeatherCode.SHOWERS) {
            return conditionCode;
        } else if (conditionCode <= SCATTERED_THUNDERSTORMS) {
            return conditionCode + 1;
        } else if (conditionCode <= SCATTERED_SNOW_SHOWERS) {
            return conditionCode + 2;
        } else if (conditionCode <= ISOLATED_THUNDERSHOWERS) {
            return conditionCode + 3;
        } else {
            return NOT_AVAILABLE;
        }
    }
}
