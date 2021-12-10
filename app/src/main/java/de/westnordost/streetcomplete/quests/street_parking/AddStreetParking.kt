package de.westnordost.streetcomplete.quests.street_parking

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.meta.MAXSPEED_TYPE_KEYS
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.user.achievements.QuestTypeAchievement.CAR
import de.westnordost.streetcomplete.osm.street_parking.*


class AddStreetParking : OsmFilterQuestType<LeftAndRightStreetParking>() {

    override val elementFilter = """
        ways with
          (
            highway ~ residential|living_street
            or (
              highway ~ primary|secondary|tertiary|unclassified
              and (
                sidewalk ~ both|left|right|yes|separate
                or ~${(MAXSPEED_TYPE_KEYS + "maxspeed").joinToString("|")} ~ .*urban|.*zone.*
                or maxspeed <= 60
                or maxspeed ~ "(5|10|15|20|25|30|35) mph"
              )
            )
          )
          and !parking:lane and !parking:lane:left and !parking:lane:right and !parking:lane:both
          and area != yes
          and motorroad != yes
          and tunnel != yes
          and bridge != yes
          and priority_road !~ designated|yes
          and overtaking !~ no|forward|backward
          and junction != roundabout
          and !turn:lanes and !turn:lanes:forward and !turn:lanes:backward and !turn:lanes:both_ways
          and (
            access !~ private|no
            or foot and foot !~ private|no
          )
    """

    /* On some roads, usually no-parking rules apply implicitly, so these are filtered out:
       - motorways, trunks (motorroads), pedestrian zones,
       - often priority roads (at least rural ones), roads where overtaking is forbidden
         (continuous center line)
       - roundabouts
       - on sections of the roadway marked with arrows (turn lanes)

       There are some more rules which cannot be filtered due to the lack of tags for that that are
       set on the road-way:
       - in front of important signs (STOP, saltires, yield etc)
       - at taxi stands, bus stops, ...
       - on and near crossings, level crossings, ... on tram tracks (duh!) etc
       - at narrow points, sharp bends, fire rescue paths and other dangerous points
       - at entries to driveways and other places where there is a dropped kerb
         (but I don't think street parking will/should be mapped at that level of detail)
       - in some country: in front of police stations, post offices, hospitals...
       - etc

       Further, to ask outside of urban areas does not really make sense, so we fuzzily exclude
       roads that are probably outside of settlements (similar idea like for AddWayLit)
      */

    override val commitMessage = "Add how cars park here"
    override val wikiLink = "Key:parking:lane"
    override val icon = R.drawable.ic_quest_parking_lane
    override val isSplitWayEnabled = true

    override val questTypeAchievements = listOf(CAR)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_street_parking_title

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter("ways with amenity = parking")

    override fun createForm() = AddStreetParkingForm()

    private enum class Side(val value: String) {
        LEFT("left"), RIGHT("right"), BOTH("both")
    }

    override fun applyAnswerTo(answer: LeftAndRightStreetParking, changes: StringMapChangesBuilder) {
        if (answer.left!! == answer.right!!) {
            applyParkingAnswerTo(answer.left, Side.BOTH, changes)
        } else {
            applyParkingAnswerTo(answer.left, Side.LEFT, changes)
            applyParkingAnswerTo(answer.right, Side.RIGHT, changes)
        }
    }

    private fun applyParkingAnswerTo(parking: StreetParking, side: Side, changes: StringMapChangesBuilder) {
        /* Note: If a resurvey is implemented, old
           parking:lane:*:(parallel|diagonal|perpendicular) values must be cleaned up */

        val parkingKey = "parking:lane:" + side.value
        when(parking) {
            is StreetParkingPositionAndOrientation -> {
                val orientation = parking.orientation.toOsmValue()
                val position = parking.position.toOsmValue()
                changes.add(parkingKey, orientation)
                changes.addOrModify("$parkingKey:$orientation", position)
            }
            StreetParkingSeparate -> {
                changes.add(parkingKey, "separate")
            }
            NoStreetParking -> {
                changes.add(parkingKey, "no")
            }
            StreetParkingProhibited -> {
                changes.add(parkingKey, "no_parking")
            }
            StreetStandingProhibited -> {
                changes.add(parkingKey, "no_standing")
            }
            StreetStoppingProhibited -> {
                changes.add(parkingKey, "no_stopping")
            }
            else -> throw IllegalArgumentException()
        }
    }
}