package bose.ankush.home.presentation.wander

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WanderAlertTextTest {
    @Test
    fun heatAdvisorySplitsIntoLabelledSectionsInOrder() {
        val parsed = parseAlertDescription(HEAT_ADVISORY)

        assertEquals(listOf("What", "Where", "When", "Impacts"), parsed.sections.map { it.label })
        assertNull(parsed.fallback)
    }

    @Test
    fun hardWrappedLinesAreJoinedWithSingleSpaces() {
        val what = parseAlertDescription(HEAT_ADVISORY).sections.first { it.label == "What" }

        assertEquals(
            "Hot conditions with maximum temperatures in the mid-90s to low-100s. Mild minimum " +
                "temperatures and weak onshore flow will limit overnight relief.",
            what.body,
        )
        val impacts = parseAlertDescription(HEAT_ADVISORY).sections.first { it.label == "Impacts" }
        assertEquals(
            "This level of heat affects anyone without effective cooling and/or adequate hydration. " +
                "Impacts likely in some health systems, heat-sensitive industries and infrastructure.",
            impacts.body,
        )
    }

    @Test
    fun whereSplitsOnCommasWithoutBreakingPlaceNames() {
        val where = parseAlertDescription(HEAT_ADVISORY).sections.first { it.label == "Where" }

        assertEquals(
            listOf(
                "The East Bay and San Francisco Bay Shoreline",
                "North Bay Interior Mountains and Valleys",
                "The Santa Clara Valley and Eastern Hills",
                "The Santa Cruz Mountains",
                "Interior Monterey County and the Santa Lucia Range",
                "Most of San Benito County and the Cholame Hills in Southeast Monterey County",
                "Southern Salinas Valley/Arroyo Seco and Lake San Antonio",
            ),
            where.items,
        )
    }

    @Test
    fun whenBecomesSummaryAndLeavesTheBodySections() {
        val parsed = parseAlertDescription(HEAT_ADVISORY)

        assertEquals("Until 10 PM PDT Wednesday.", parsed.summary)
        assertEquals(listOf("What", "Where", "Impacts"), parsed.bodySections.map { it.label })
    }

    @Test
    fun textWithoutMarkersFallsBackToCleanedWholeText() {
        val parsed = parseAlertDescription("Strong winds expected\nthis evening.\n\nSecure loose   objects.")

        assertTrue(parsed.sections.isEmpty())
        assertEquals("Strong winds expected this evening.\n\nSecure loose objects.", parsed.fallback)
        assertNull(parsed.summary)
    }

    @Test
    fun blankOrMissingDescriptionParsesToNothing() {
        listOf(null, "", "   \n\n  ").forEach { raw ->
            val parsed = parseAlertDescription(raw)
            assertTrue(parsed.sections.isEmpty())
            assertNull(parsed.fallback)
        }
    }

    @Test
    fun markersTolerateCaseSpacingAndMultiWordLabels() {
        val parsed =
            parseAlertDescription(
                "Issued for the coast.\n\n * what ...Rain.\n\n*  ADDITIONAL DETAILS...Roads may\nflood.",
            )

        assertEquals(listOf(null, "What", "Additional details"), parsed.sections.map { it.label })
        assertEquals("Issued for the coast.", parsed.sections[0].body)
        assertEquals("Roads may flood.", parsed.sections[2].body)
    }

    @Test
    fun whereWithOneAreaStaysAParagraph() {
        assertNull(splitAlertAreas("Coastal Marin County."))
    }

    @Test
    fun panelShowsWhatThenImpactsWithoutHeadingsBeforeWhere() {
        val display = parseAlertDescription(HEAT_ADVISORY).displaySections()

        assertEquals(listOf("What", "Impacts", "Where"), display.map { it.section.label })
        assertNull(display[0].heading)
        assertNull(display[1].heading)
    }

    @Test
    fun whereIsHeadedImpactedAreasAndKeepsItsAreaList() {
        val where = parseAlertDescription(HEAT_ADVISORY).displaySections().single { it.section.label == "Where" }

        assertEquals("Impacted areas", where.heading)
        assertEquals(7, where.section.items?.size)
    }

    @Test
    fun otherSectionsFollowWhereWithTheirOwnHeadingsInOriginalOrder() {
        val display =
            parseAlertDescription(
                "Issued for the coast.\n\n* WHERE...Marin, Sonoma.\n\n* ADDITIONAL DETAILS...Roads may flood." +
                    "\n\n* WHEN...Until noon.\n\n* WHAT...Rain.",
            ).displaySections()

        assertEquals(
            listOf("What", "Where", null, "Additional details"),
            display.map { it.section.label },
        )
        assertEquals(listOf(null, "Impacted areas", null, "Additional details"), display.map { it.heading })
    }

    private companion object {
        const val HEAT_ADVISORY =
            "* WHAT...Hot conditions with maximum temperatures in the mid-90s to\nlow-100s. Mild minimum " +
                "temperatures and weak onshore flow will\nlimit overnight relief.\n\n* WHERE...The East Bay " +
                "and San Francisco Bay Shoreline, North Bay\nInterior Mountains and Valleys, The Santa Clara " +
                "Valley and Eastern\nHills, The Santa Cruz Mountains, Interior Monterey County and the\nSanta " +
                "Lucia Range, Most of San Benito County and the Cholame Hills\nin Southeast Monterey County, " +
                "and Southern Salinas Valley/Arroyo\nSeco and Lake San Antonio.\n\n* WHEN...Until 10 PM PDT " +
                "Wednesday.\n\n* IMPACTS...This level of heat affects anyone without effective\ncooling and/or " +
                "adequate hydration. Impacts likely in some health\nsystems, heat-sensitive industries and " +
                "infrastructure."
    }
}
