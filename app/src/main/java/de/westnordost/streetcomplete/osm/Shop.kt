package de.westnordost.streetcomplete.osm

import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.mapdata.Element

/** Return whether this element is a kind of shop, regardless whether it is currently vacant or
 *  not */
fun Element.isShopOrDisusedShop(): Boolean =
    isShop() || isDisusedShop()

/** Return whether this element is a kind of disused or vacant shop */
fun Element.isDisusedShop(): Boolean =
    IS_VACANT_SHOP_EXPRESSION.matches(this) ||
    this.asIfItWasnt("disused")?.let { IS_SHOP_EXPRESSION.matches(it) } == true

fun Element.isShop(): Boolean =
    IS_SHOP_EXPRESSION.matches(this)

/** Tenant of retail or commercial rooms, e.g. a shop, an office etc.
 *  Something that can occupy (a part) of a non-purpose-built building
 *
 *  So, no larger purpose-built things like malls, cinemas, theatres, zoos, aquariums,
 *  bowling alleys...
 *
 *  Note: When this function is modified, please follow update instructions in:
 *  https://github.com/mnalis/StreetComplete-taginfo-categorize/blob/master/README.md
 *  */
private val IS_SHOP_EXPRESSION by lazy { ("""
    nodes, ways, relations with
    shop and shop !~ no|vacant|mall
    or office and office !~ no|vacant
    or healthcare and healthcare != hospital
    or craft
    or club
    or tourism = information and information = office
    or amenity = social_facility and social_facility ~ ${
        listOf(
            // only non-residential ones
            "ambulatory_care",
            "clothing_bank",
            "dairy_kitchen",
            "day_care",
            "food_bank",
            "healthcare",
            "outreach",
            "soup_kitchen",
            "workshop"
        ).joinToString("|")
    }
    or """ + mapOf(
        "leisure" to listOf(
            "adult_gaming_centre",
            "amusement_arcade",
            // "bowling_alley", // purpose-built
            "dance", // not necessarily purpose-built, see fitness centre
            "dancing_school",
            "escape_game",
            // "ice_rink", // purpose-built
            "indoor_play",
            "fitness_centre", // not necessarily purpose-built, esp. the smaller ones
            "hackerspace",
            // "resort", // a kind of hotel+theme/water/whatever park
            "sauna",
            // "sports_centre", // purpose-built
            "tanning_salon",
            // "trampoline_park", // see sports centre
            // "water_park" // purpose-built
        ),
        "tourism" to listOf(
            // tourism = information only if it is an office, see above
            // purpose-built
            // "aquarium",
            // "zoo",
            // "theme_park",
            "gallery", // could be just an artist's show-room
            "museum", // only the larger ones are purpose-built
            // tourist accommodations are usually not in something that could otherwise be just a showroom, office etc.
            // "alpine_hut",
            // "apartment",
            // "chalet",
            // "camp_site",
            // "guest_house",
            // "hostel",
            // "hotel",
            // "motel",

        ),
        "amenity" to listOf(
            /* amenity, the "garbage patch in the OSM ocean" - https://media.ccc.de/v/sotm2022-18515-every-door-and-the-future-of-poi-in-openstreetmap#t=528
               sorted by occurrence on wiki page Key:amenity */
            /* sustenance */
            "bar",
            "biergarten",
            "cafe",
            "fast_food",
            "food_court",
            "ice_cream",
            "internet_cafe",
            "pub",
            "restaurant",

            /* education */
            "childcare",
            // "college", purpose-built
            "dive_centre", // depends, but can be in a showroom just like a driving school
            "dojo", // same as fitness_centre
            "driving_school",
            "kindergarten",
            "language_school",
            "library",
            "music_school",
            "prep_school",
            // "research_institute", purpose-built
            // "school", purpose-built
            "toy_library",
            "training",
            // "university", purpose-built

            /* transportation */
            // "bicycle_rental", // usually outside, could be automatic too
            // "boat_rental", // usually outside, could be automatic too
            // "ski_rental", // seems borderline
            "car_rental",
            "car_wash", // purpose-built, but see fuel
            "fuel", // purpose-built but too much of a shop that it would be weird to leave out
            "motorcycle_rental",
            "vehicle_inspection", // often similar to a car repair shop

            /* financial */
            "bank",
            "bureau_de_change",
            "money_transfer",
            "payment_centre",

            /* healthcare */
            "clinic", // sizes vary a lot, not necessarily purpose-built
            // "crematorium", // purpose-built
            "dentist",
            "doctors",
            // "mortuary", // purpose-built
            // "hospital", // purpose-built
            "pharmacy",
            // "social_facility", only if it is not residential, see above
            "veterinary",

            /* entertainment, arts & culture */
            "arts_centre",
            "brothel",
            "casino", // usually purpose-built, but doesn't have to be
            // "cinema", // typically purpose-built
            "community_centre", // often purpose-built, but not necessarily
            // "conference_centre", // purpose-built
            "events_venue", // smaller ones are not purpose-built
            // "exhibition_centre", // purpose-built
            "gambling",
            // "love_hotel",
            // "planetarium", // like cinema
            "nightclub",
            "social_centre",
            "stripclub",
            "studio",
            "swingerclub",
            // "theatre",

            /* public service */
            // "courthouse", // purpose-built
            // "fire_station", // purpose-built
            // "police", // purpose-built
            "post_office",
            // "ranger_station", // like police station
            // "townhall", // purpose-built

            /* other */
            "animal_boarding", // not necessarily purpose-built
            // "animal_breeding",
            "animal_shelter", // not necessarily purpose-built
            "coworking_space", // basically an office
            // "embassy", // usually purpose-built / there is also office=diplomatic for those that have services
            // "monastery", // purpose-built, often historic
            "place_of_worship" // usually-purpose-built, but not always (also, prayer rooms)
        )
    )
    .map { it.key + " ~ " + it.value.joinToString("|") }
    .joinToString("\n    or ")
).toElementFilterExpression()
}

/** Expression to see if an element is some kind of vacant shop */
private val IS_VACANT_SHOP_EXPRESSION = """
    nodes, ways, relations with
      shop = vacant
      or office = vacant
      or amenity = vacant
""".toElementFilterExpression()

/** iD preset ids of popular shop types */
val POPULAR_SHOP_FEATURE_IDS = listOf(
    // ordered roughly by usage number according to taginfo
    "amenity/restaurant",
    "shop/convenience",
    "amenity/cafe",
    "shop/supermarket",
    "amenity/fast_food",
    "amenity/pharmacy",
    "shop/clothes",
    "shop/hairdresser"
)

/** Replace a shop with the given new tags.
 *  Removes any shop-related tags before adding the given [tags]. */
fun StringMapChangesBuilder.replaceShop(tags: Map<String, String>) {
    removeCheckDates()

    for (key in keys) {
        if (KEYS_THAT_SHOULD_BE_REMOVED_WHEN_SHOP_IS_REPLACED.any { it.matches(key) }) {
            remove(key)
        }
    }

    for ((key, value) in tags) {
        this[key] = value
    }
}


// generated by "make update" from https://github.com/mnalis/StreetComplete-taginfo-categorize/
private val KEYS_THAT_SHOULD_BE_REMOVED_WHEN_SHOP_IS_REPLACED = listOf(
    "shop_?[1-9]?(:.*)?", "craft_?[1-9]?", "amenity_?[1-9]?", "old_amenity", "old_shop",
    "information", "leisure", "office_?[1-9]?", "tourism",
    // popular shop=* / craft=* subkeys
    "marketplace", "household", "swimming_pool", "laundry", "golf", "sports", "ice_cream",
    "scooter", "music", "retail", "yes", "ticket", "newsagent", "lighting", "truck", "car_repair",
    "car_parts", "video", "fuel", "farm", "car", "tractor", "hgv", "ski", "sculptor",
    "hearing_aids", "surf", "photo", "boat", "gas", "kitchen", "anime", "builder", "hairdresser",
    "security", "bakery", "bakehouse", "fishing", "doors", "kiosk", "market", "bathroom", "lamps",
    "vacant", "insurance(:.*)?", "caravan", "gift", "bicycle", "bicycle_rental", "insulation",
    "communication", "mall", "model", "empty", "wood", "hunting", "motorcycle", "trailer",
    "camera", "water", "fireplace", "outdoor", "blacksmith",
    // obsoleted information
    "abandoned(:.*)?", "disused(:.*)?", "was:.*", "not:.*", "damage", "source:damage",
    "created_by", "check_date", "opening_date", "last_checked", "checked_exists:date",
    "pharmacy_survey", "old_ref", "update", "import_uuid",
    // classifications / links to external databases
    "fhrs:.*", "old_fhrs:.*", "fvst:.*", "ncat", "nat_ref", "gnis:.*", "winkelnummer",
    "type:FR:FINESS", "type:FR:APE", "kvl_hro:amenity", "ref:DK:cvr(:.*)?", "certifications?",
    "transiscope", "opendata:type",
    // names and identifications
    "name_?[1-9]?(:.*)?", ".*_name_?[1-9]?(:.*)?", "noname", "branch(:.*)?", "brand(:.*)?",
    "not:brand(:.*)?", "network", "operator(:.*)?", "operator_type", "ref", "ref:vatin",
    "designation", "SEP:CLAVEESC", "identifier",
    // contacts
    "contact_person", "contact(:.*)?", "phone(:.*)?", "phone_?[1-9]?", "emergency:phone", "mobile",
    "fax", "facebook", "instagram", "twitter", "youtube", "telegram", "email",
    "website_?[1-9]?(:.*)?", "app:.*", "ownership",
    "url", "source_ref:url", "owner",
    // payments
    "payment(:.*)?", "payment_multi_fee", "currency:.*", "cash_withdrawal(:.*)?", "fee", "charge",
    "charge_fee", "money_transfer", "donation:compensation",
    // generic shop/craft attributes
    "seasonal", "time", "opening_hours(:.*)?", "check_date:opening_hours", "wifi", "internet",
    "internet_access(:.*)?", "second_hand", "self_service", "automated", "license:.*",
    "bulk_purchase", ".*:covid19", "language:.*", "baby_feeding", "description(:.*)?",
    "description[0-9]", "min_age", "max_age", "supermarket(:.*)?", "social_facility(:.*)?",
    "functional", "trade", "wholesale", "sale", "smoking", "zero_waste", "origin", "attraction",
    "strapline", "dog", "showroom", "toilets?(:.*)?", "changing_table", "wheelchair(.*)?", "blind",
    "company(:.*)?", "stroller", "walk-in", "webshop", "operational_status.*", "drive_through",
    "surveillance(:.*)?", "outdoor_seating", "indoor_seating", "colour", "access_simple", "floor",
    "product_category", "source_url", "category", "kids_area", "resort", "since", "state",
    "operational_status", "temporary",
    // food and drink details
    "bar", "cafe", "coffee", "microroasting", "microbrewery", "brewery", "real_ale", "taproom",
    "training", "distillery", "drink(:.*)?", "cocktails", "alcohol", "wine([:_].*)?",
    "happy_hours", "diet:.*", "cuisine", "tasting", "breakfast", "lunch", "organic",
    "produced_on_site", "restaurant", "food", "pastry", "pastry_shop", "product", "produce",
    "chocolate", "fair_trade", "butcher", "reservation(:.*)?", "takeaway(:.*)?", "delivery(:.*)?",
    "caterer", "real_fire", "flour_fortified", "highchair", "sport_pub",
    // related to repair shops/crafts
    "service(:.*)?", "motorcycle:.*", "repair", ".*:repair", "electronics_repair(:.*)?",
    "workshop",
    // shop=hairdresser, shop=clothes
    "unisex", "male", "female", "gender", "lgbtq(:.*)?",
    // healthcare
    "healthcare(:.*)?", "health", "health_.*", "medical_.*", "facility(:.*)?", "activities",
    "healthcare_facility(:.*)?", "laboratory(:.*)?", "blood(:.*)?", "blood_components",
    "infection(:.*)?", "disease(:.*)?", "covid19(:.*)?", "CovidVaccineCenterId",
    "coronaquarantine", "hospital(:.*)?", "hospital_type_id", "emergency_room",
    "sample_collection(:.*)?", "bed_count", "capacity:beds", "part_time_beds", "personnel:count",
    "staff_count(:.*)?", "admin_staff", "doctors_num", "nurses_num", "counselling_type",
    "testing_centres", "toilets_number", "urgent_care", "vaccination", "clinic", "hospital",
    "pharmacy", "laboratory", "sample_collection", "provided_for(:.*)?", "social_facility_for",
    "ambulance", "ward", "HSE_(code|hgid|hgroup|region)", "collection_centre", "design",
    "AUTORIZATIE", "reg_id", "scope", "ESTADO", "NIVSOCIO", "NO", "EMP_EST", "COD_HAB", "CLA_PERS",
    "CLA_PRES", "snis_code:.*", "hfac_bed", "hfac_type", "nature", "moph_code", "IJSN:.*",
    "massgis:id", "OGD-Stmk:.*", "paho:.*", "panchayath", "pbf_contract", "pcode", "pe:minsa:.*",
    "who:.*",
    // accommodation & layout
    "rooms", "stars", "accommodation", "beds", "capacity(:persons)?", "laundry_service",
    // misc specific attributes
    "clothes", "shoes", "tailor", "beauty", "tobacco", "carpenter", "furniture", "lottery",
    "sport", "leisure", "dispensing", "tailor:.*", "gambling", "material", "raw_material",
    "stonemason", "studio", "scuba_diving(:.*)?", "polling_station", "club", "collector", "books",
    "agrarian", "musical_instrument", "massage", "parts", "post_office(:.*)?", "religion",
    "denomination", "rental", ".*:rental", "tickets:.*", "public_transport", "goods_supply", "pet",
    "appliance", "artwork_type", "charity", "company", "crop", "dry_cleaning", "factory",
    "feature", "air_conditioning", "atm", "vending", "vending_machine", "recycling_type", "museum",
    "license_classes", "dance:style", "isced:level", "school", "preschool", "university",
    "research_institution", "research", "member_of", "topic", "townhall:type", "parish", "police",
    "government", "office", "administration", "administrative", "association", "transport",
    "utility", "consulting", "commercial", "private", "taxi", "admin_level", "official_status",
    "target", "liaison", "diplomatic(:.*)?", "embassy", "consulate", "aeroway", "department",
    "faculty", "aerospace:product", "boundary", "population", "diocese", "depot", "cargo",
    "function", "game", "party", "telecom(munication)?", "service_times", "kitchen:facilities",
    "it:(type|sales)", "cannabis:cbd",
    "camp_site", "camping", "emergency(:.*)?", "evacuation_cent(er|re)", "education",
    "engineering", "forestry", "foundation", "lawyer", "logistics", "military", "community_centre",
    "bank",
).map { it.toRegex() }
