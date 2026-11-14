package com.brewlab.domain;

import com.brewlab.domain.calc.BrewRatio;
import com.brewlab.domain.calc.RoastAge;
import com.brewlab.domain.format.Formats;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainCalcTest {

    @Test
    void ratio_of_250g_water_and_15g_dose_is_16_666_rounded_to_1_16_7() {
        BrewRatio ratio = BrewRatio.of(Double.valueOf(250.0), Double.valueOf(15.0));
        assertEquals(BrewRatio.Status.OK, ratio.getStatus());
        assertTrue(ratio.isComputed());
        assertNotNull(ratio.getValue());
        assertEquals(16.6666666, ratio.getValue().doubleValue(), 0.0001);
        assertEquals(new BigDecimal("16.7"), ratio.getRounded());
        assertEquals("1:16,7", ratio.format(Formats.LOCALE));
    }

    @Test
    void ratio_blocks_zero_or_negative_dose_and_never_returns_zero_for_missing_data() {
        BrewRatio zeroDose = BrewRatio.of(Double.valueOf(250.0), Double.valueOf(0.0));
        assertEquals(BrewRatio.Status.INVALID_DOSE, zeroDose.getStatus());
        assertFalse(zeroDose.isComputed());
        assertNull(zeroDose.getValue());
        assertNull(zeroDose.format(Formats.LOCALE));

        BrewRatio missingWater = BrewRatio.of(null, Double.valueOf(15.0));
        assertEquals(BrewRatio.Status.MISSING_DATA, missingWater.getStatus());
        assertNull(missingWater.getValue());
        assertNull(missingWater.format(Formats.LOCALE));
    }

    @Test
    void roast_age_handles_today_yesterday_future_and_null() {
        LocalDate today = LocalDate.of(2026, 11, 13);

        RoastAge sameDay = RoastAge.between(today, today);
        assertEquals(RoastAge.Status.OK, sameDay.getStatus());
        assertEquals(Long.valueOf(0L), sameDay.getDays());
        assertFalse(sameDay.isFuture());

        RoastAge yesterday = RoastAge.between(today.minusDays(1), today);
        assertEquals(RoastAge.Status.OK, yesterday.getStatus());
        assertEquals(Long.valueOf(1L), yesterday.getDays());

        RoastAge future = RoastAge.between(today.plusDays(3), today);
        assertEquals(RoastAge.Status.FUTURE_DATE, future.getStatus());
        assertTrue(future.isFuture());
        assertEquals(Long.valueOf(-3L), future.getDays());

        RoastAge unknown = RoastAge.between(null, today);
        assertEquals(RoastAge.Status.UNKNOWN, unknown.getStatus());
        assertNull(unknown.getDays());
    }

    @Test
    void formats_never_render_null_as_zero() {
        assertEquals(Formats.UNKNOWN, Formats.number(null, 2));
        assertEquals(Formats.UNKNOWN, Formats.withUnit(null, 1, "g"));
        assertEquals(Formats.UNKNOWN, Formats.duration(null));
        assertEquals(Formats.UNKNOWN, Formats.text(null));
        assertEquals(Formats.UNKNOWN, Formats.text("   "));
        assertEquals("0", Formats.number(Integer.valueOf(0), 1));
    }
}
