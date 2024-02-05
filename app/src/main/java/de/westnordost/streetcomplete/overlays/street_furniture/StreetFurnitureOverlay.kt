package de.westnordost.streetcomplete.overlays.street_furniture

import de.westnordost.osmfeatures.Feature
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Node
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement
import de.westnordost.streetcomplete.osm.asIfItWasnt
import de.westnordost.streetcomplete.osm.isDisusedStreetFurniture
import de.westnordost.streetcomplete.osm.isStreetFurniture
import de.westnordost.streetcomplete.overlays.Color
import de.westnordost.streetcomplete.overlays.Overlay
import de.westnordost.streetcomplete.overlays.PointStyle
import de.westnordost.streetcomplete.overlays.PolygonStyle

class StreetFurnitureOverlay(private val getFeature: (Element) -> Feature?) : Overlay {

    override val title = R.string.overlay_street_furniture
    override val icon = R.drawable.ic_quest_bench_poi
    override val changesetComment = "Survey street furniture and similar objects"
    override val wikiLink: String = "Street furniture"
    override val achievements = listOf(EditTypeAchievement.CITIZEN)
    override val isCreateNodeEnabled = true

    override val sceneUpdates = listOf(
        "layers.buildings.draw.buildings-style.extrude" to "false",
        "layers.buildings.draw.buildings-outline-style.extrude" to "false"
    )

    override fun getStyledElements(mapData: MapDataWithGeometry) =
        mapData
            .asSequence()
            .filter { it.isStreetFurniture() || it.isDisusedStreetFurniture() }
            .mapNotNull { element ->
                val feature = getFeature(element)
                    ?: element.asIfItWasnt("disused")?.let { getFeature(it) }
                    ?: return@mapNotNull null

                val icon = "ic_preset_" + (feature.icon ?: "maki-marker-stroked").replace('-', '_')

                val style = if (element is Node) {
                    PointStyle(icon)
                } else {
                    PolygonStyle(Color.INVISIBLE, icon)
                }
                element to style
            }

    override fun createForm(element: Element?) = StreetFurnitureOverlayForm()
}
