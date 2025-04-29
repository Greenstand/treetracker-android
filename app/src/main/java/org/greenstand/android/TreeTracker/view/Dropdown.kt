package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hbb20.CCPCountry
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.countries.CountryPickerData
import java.util.Locale

/**
 * @param onClick The callback function for click event.
 * @param modifier The modifier to be applied to the layout.
 * @param approval Set the type of button to display(if approval is true, shows green thumps up button )
 */
@Composable
fun CountryPickerDropdown(
    modifier: Modifier = Modifier,
    onItemClick: (CountryPickerData) -> Unit,
    countryList: List<CountryPickerData>,
    defaultCountryCode: String,
    onDropdownDismiss: () -> Unit
) {

    var selectedCountry by remember { mutableStateOf<CountryPickerData?>(null) }

    DropdownMenu(
        expanded = true,
        onDismissRequest = { onDropdownDismiss() },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Color(R.color.colorPrimary))
            .zIndex(2f)
    ) {
        Box(modifier = Modifier.size(width = 300.dp, height = 300.dp)) {
            LazyColumn {
                items(countryList) { option ->
                    DropdownMenuItem(onClick = {
                        selectedCountry = option
                        onItemClick(option)
                        onDropdownDismiss()
                    }) {
                        Row {
                            option.countryFlag?.let { painterResource(id = it) }
                                ?.let { Image(painter = it, contentDescription = "") }
                            Spacer(Modifier.width(8.dp))
                            Text(text = option.countryCode, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(text = option.countryName, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

fun getFlagMasterResID(CCPCountry: CCPCountry): Int {
    return when (CCPCountry.nameCode.lowercase(Locale.getDefault())) {
        "ad" -> com.hbb20.R.drawable.flag_andorra
        "ae" -> com.hbb20.R.drawable.flag_uae
        "af" -> com.hbb20.R.drawable.flag_afghanistan
        "ag" -> com.hbb20.R.drawable.flag_antigua_and_barbuda
        "ai" -> com.hbb20.R.drawable.flag_anguilla
        "al" -> com.hbb20.R.drawable.flag_albania
        "am" -> com.hbb20.R.drawable.flag_armenia
        "ao" -> com.hbb20.R.drawable.flag_angola
        "aq" -> com.hbb20.R.drawable.flag_antarctica
        "ar" -> com.hbb20.R.drawable.flag_argentina
        "as" -> com.hbb20.R.drawable.flag_american_samoa
        "at" -> com.hbb20.R.drawable.flag_austria
        "au" -> com.hbb20.R.drawable.flag_australia
        "aw" -> com.hbb20.R.drawable.flag_aruba
        "ax" -> com.hbb20.R.drawable.flag_aland
        "az" -> com.hbb20.R.drawable.flag_azerbaijan
        "ba" -> com.hbb20.R.drawable.flag_bosnia
        "bb" -> com.hbb20.R.drawable.flag_barbados
        "bd" -> com.hbb20.R.drawable.flag_bangladesh
        "be" -> com.hbb20.R.drawable.flag_belgium
        "bf" -> com.hbb20.R.drawable.flag_burkina_faso
        "bg" -> com.hbb20.R.drawable.flag_bulgaria
        "bh" -> com.hbb20.R.drawable.flag_bahrain
        "bi" -> com.hbb20.R.drawable.flag_burundi
        "bj" -> com.hbb20.R.drawable.flag_benin
        "bl" -> com.hbb20.R.drawable.flag_saint_barthelemy // custom
        "bm" -> com.hbb20.R.drawable.flag_bermuda
        "bn" -> com.hbb20.R.drawable.flag_brunei
        "bo" -> com.hbb20.R.drawable.flag_bolivia
        "br" -> com.hbb20.R.drawable.flag_brazil
        "bs" -> com.hbb20.R.drawable.flag_bahamas
        "bt" -> com.hbb20.R.drawable.flag_bhutan
        "bw" -> com.hbb20.R.drawable.flag_botswana
        "by" -> com.hbb20.R.drawable.flag_belarus
        "bz" -> com.hbb20.R.drawable.flag_belize
        "ca" -> com.hbb20.R.drawable.flag_canada
        "cc" -> com.hbb20.R.drawable.flag_cocos // custom
        "cd" -> com.hbb20.R.drawable.flag_democratic_republic_of_the_congo
        "cf" -> com.hbb20.R.drawable.flag_central_african_republic
        "cg" -> com.hbb20.R.drawable.flag_republic_of_the_congo
        "ch" -> com.hbb20.R.drawable.flag_switzerland
        "ci" -> com.hbb20.R.drawable.flag_cote_divoire
        "ck" -> com.hbb20.R.drawable.flag_cook_islands
        "cl" -> com.hbb20.R.drawable.flag_chile
        "cm" -> com.hbb20.R.drawable.flag_cameroon
        "cn" -> com.hbb20.R.drawable.flag_china
        "co" -> com.hbb20.R.drawable.flag_colombia
        "cr" -> com.hbb20.R.drawable.flag_costa_rica
        "cu" -> com.hbb20.R.drawable.flag_cuba
        "cv" -> com.hbb20.R.drawable.flag_cape_verde
        "cw" -> com.hbb20.R.drawable.flag_curacao
        "cx" -> com.hbb20.R.drawable.flag_christmas_island
        "cy" -> com.hbb20.R.drawable.flag_cyprus
        "cz" -> com.hbb20.R.drawable.flag_czech_republic
        "de" -> com.hbb20.R.drawable.flag_germany
        "dj" -> com.hbb20.R.drawable.flag_djibouti
        "dk" -> com.hbb20.R.drawable.flag_denmark
        "dm" -> com.hbb20.R.drawable.flag_dominica
        "do" -> com.hbb20.R.drawable.flag_dominican_republic
        "dz" -> com.hbb20.R.drawable.flag_algeria
        "ec" -> com.hbb20.R.drawable.flag_ecuador
        "ee" -> com.hbb20.R.drawable.flag_estonia
        "eg" -> com.hbb20.R.drawable.flag_egypt
        "er" -> com.hbb20.R.drawable.flag_eritrea
        "es" -> com.hbb20.R.drawable.flag_spain
        "et" -> com.hbb20.R.drawable.flag_ethiopia
        "fi" -> com.hbb20.R.drawable.flag_finland
        "fj" -> com.hbb20.R.drawable.flag_fiji
        "fk" -> com.hbb20.R.drawable.flag_falkland_islands
        "fm" -> com.hbb20.R.drawable.flag_micronesia
        "fo" -> com.hbb20.R.drawable.flag_faroe_islands
        "fr" -> com.hbb20.R.drawable.flag_france
        "ga" -> com.hbb20.R.drawable.flag_gabon
        "gb" -> com.hbb20.R.drawable.flag_united_kingdom
        "gd" -> com.hbb20.R.drawable.flag_grenada
        "ge" -> com.hbb20.R.drawable.flag_georgia
        "gf" -> com.hbb20.R.drawable.flag_guyane
        "gg" -> com.hbb20.R.drawable.flag_guernsey
        "gh" -> com.hbb20.R.drawable.flag_ghana
        "gi" -> com.hbb20.R.drawable.flag_gibraltar
        "gl" -> com.hbb20.R.drawable.flag_greenland
        "gm" -> com.hbb20.R.drawable.flag_gambia
        "gn" -> com.hbb20.R.drawable.flag_guinea
        "gp" -> com.hbb20.R.drawable.flag_guadeloupe
        "gq" -> com.hbb20.R.drawable.flag_equatorial_guinea
        "gr" -> com.hbb20.R.drawable.flag_greece
        "gt" -> com.hbb20.R.drawable.flag_guatemala
        "gu" -> com.hbb20.R.drawable.flag_guam
        "gw" -> com.hbb20.R.drawable.flag_guinea_bissau
        "gy" -> com.hbb20.R.drawable.flag_guyana
        "hk" -> com.hbb20.R.drawable.flag_hong_kong
        "hn" -> com.hbb20.R.drawable.flag_honduras
        "hr" -> com.hbb20.R.drawable.flag_croatia
        "ht" -> com.hbb20.R.drawable.flag_haiti
        "hu" -> com.hbb20.R.drawable.flag_hungary
        "id" -> com.hbb20.R.drawable.flag_indonesia
        "ie" -> com.hbb20.R.drawable.flag_ireland
        "il" -> com.hbb20.R.drawable.flag_israel
        "im" -> com.hbb20.R.drawable.flag_isleof_man // custom
        "is" -> com.hbb20.R.drawable.flag_iceland
        "in" -> com.hbb20.R.drawable.flag_india
        "io" -> com.hbb20.R.drawable.flag_british_indian_ocean_territory
        "iq" -> com.hbb20.R.drawable.flag_iraq_new
        "ir" -> com.hbb20.R.drawable.flag_iran
        "it" -> com.hbb20.R.drawable.flag_italy
        "je" -> com.hbb20.R.drawable.flag_jersey
        "jm" -> com.hbb20.R.drawable.flag_jamaica
        "jo" -> com.hbb20.R.drawable.flag_jordan
        "jp" -> com.hbb20.R.drawable.flag_japan
        "ke" -> com.hbb20.R.drawable.flag_kenya
        "kg" -> com.hbb20.R.drawable.flag_kyrgyzstan
        "kh" -> com.hbb20.R.drawable.flag_cambodia
        "ki" -> com.hbb20.R.drawable.flag_kiribati
        "km" -> com.hbb20.R.drawable.flag_comoros
        "kn" -> com.hbb20.R.drawable.flag_saint_kitts_and_nevis
        "kp" -> com.hbb20.R.drawable.flag_north_korea
        "kr" -> com.hbb20.R.drawable.flag_south_korea
        "kw" -> com.hbb20.R.drawable.flag_kuwait
        "ky" -> com.hbb20.R.drawable.flag_cayman_islands
        "kz" -> com.hbb20.R.drawable.flag_kazakhstan
        "la" -> com.hbb20.R.drawable.flag_laos
        "lb" -> com.hbb20.R.drawable.flag_lebanon
        "lc" -> com.hbb20.R.drawable.flag_saint_lucia
        "li" -> com.hbb20.R.drawable.flag_liechtenstein
        "lk" -> com.hbb20.R.drawable.flag_sri_lanka
        "lr" -> com.hbb20.R.drawable.flag_liberia
        "ls" -> com.hbb20.R.drawable.flag_lesotho
        "lt" -> com.hbb20.R.drawable.flag_lithuania
        "lu" -> com.hbb20.R.drawable.flag_luxembourg
        "lv" -> com.hbb20.R.drawable.flag_latvia
        "ly" -> com.hbb20.R.drawable.flag_libya
        "ma" -> com.hbb20.R.drawable.flag_morocco
        "mc" -> com.hbb20.R.drawable.flag_monaco
        "md" -> com.hbb20.R.drawable.flag_moldova
        "me" -> com.hbb20.R.drawable.flag_of_montenegro // custom
        "mf" -> com.hbb20.R.drawable.flag_saint_martin
        "mg" -> com.hbb20.R.drawable.flag_madagascar
        "mh" -> com.hbb20.R.drawable.flag_marshall_islands
        "mk" -> com.hbb20.R.drawable.flag_macedonia
        "ml" -> com.hbb20.R.drawable.flag_mali
        "mm" -> com.hbb20.R.drawable.flag_myanmar
        "mn" -> com.hbb20.R.drawable.flag_mongolia
        "mo" -> com.hbb20.R.drawable.flag_macao
        "mp" -> com.hbb20.R.drawable.flag_northern_mariana_islands
        "mq" -> com.hbb20.R.drawable.flag_martinique
        "mr" -> com.hbb20.R.drawable.flag_mauritania
        "ms" -> com.hbb20.R.drawable.flag_montserrat
        "mt" -> com.hbb20.R.drawable.flag_malta
        "mu" -> com.hbb20.R.drawable.flag_mauritius
        "mv" -> com.hbb20.R.drawable.flag_maldives
        "mw" -> com.hbb20.R.drawable.flag_malawi
        "mx" -> com.hbb20.R.drawable.flag_mexico
        "my" -> com.hbb20.R.drawable.flag_malaysia
        "mz" -> com.hbb20.R.drawable.flag_mozambique
        "na" -> com.hbb20.R.drawable.flag_namibia
        "nc" -> com.hbb20.R.drawable.flag_new_caledonia // custom
        "ne" -> com.hbb20.R.drawable.flag_niger
        "nf" -> com.hbb20.R.drawable.flag_norfolk_island
        "ng" -> com.hbb20.R.drawable.flag_nigeria
        "ni" -> com.hbb20.R.drawable.flag_nicaragua
        "nl" -> com.hbb20.R.drawable.flag_netherlands
        "no" -> com.hbb20.R.drawable.flag_norway
        "np" -> com.hbb20.R.drawable.flag_nepal
        "nr" -> com.hbb20.R.drawable.flag_nauru
        "nu" -> com.hbb20.R.drawable.flag_niue
        "nz" -> com.hbb20.R.drawable.flag_new_zealand
        "om" -> com.hbb20.R.drawable.flag_oman
        "pa" -> com.hbb20.R.drawable.flag_panama
        "pe" -> com.hbb20.R.drawable.flag_peru
        "pf" -> com.hbb20.R.drawable.flag_french_polynesia
        "pg" -> com.hbb20.R.drawable.flag_papua_new_guinea
        "ph" -> com.hbb20.R.drawable.flag_philippines
        "pk" -> com.hbb20.R.drawable.flag_pakistan
        "pl" -> com.hbb20.R.drawable.flag_poland
        "pm" -> com.hbb20.R.drawable.flag_saint_pierre
        "pn" -> com.hbb20.R.drawable.flag_pitcairn_islands
        "pr" -> com.hbb20.R.drawable.flag_puerto_rico
        "ps" -> com.hbb20.R.drawable.flag_palestine
        "pt" -> com.hbb20.R.drawable.flag_portugal
        "pw" -> com.hbb20.R.drawable.flag_palau
        "py" -> com.hbb20.R.drawable.flag_paraguay
        "qa" -> com.hbb20.R.drawable.flag_qatar
        "re" -> com.hbb20.R.drawable.flag_martinique // no exact flag found
        "ro" -> com.hbb20.R.drawable.flag_romania
        "rs" -> com.hbb20.R.drawable.flag_serbia // custom
        "ru" -> com.hbb20.R.drawable.flag_russian_federation
        "rw" -> com.hbb20.R.drawable.flag_rwanda
        "sa" -> com.hbb20.R.drawable.flag_saudi_arabia
        "sb" -> com.hbb20.R.drawable.flag_soloman_islands
        "sc" -> com.hbb20.R.drawable.flag_seychelles
        "sd" -> com.hbb20.R.drawable.flag_sudan
        "se" -> com.hbb20.R.drawable.flag_sweden
        "sg" -> com.hbb20.R.drawable.flag_singapore
        "sh" -> com.hbb20.R.drawable.flag_saint_helena // custom
        "si" -> com.hbb20.R.drawable.flag_slovenia
        "sk" -> com.hbb20.R.drawable.flag_slovakia
        "sl" -> com.hbb20.R.drawable.flag_sierra_leone
        "sm" -> com.hbb20.R.drawable.flag_san_marino
        "sn" -> com.hbb20.R.drawable.flag_senegal
        "so" -> com.hbb20.R.drawable.flag_somalia
        "sr" -> com.hbb20.R.drawable.flag_suriname
        "ss" -> com.hbb20.R.drawable.flag_south_sudan
        "st" -> com.hbb20.R.drawable.flag_sao_tome_and_principe
        "sv" -> com.hbb20.R.drawable.flag_el_salvador
        "sx" -> com.hbb20.R.drawable.flag_sint_maarten
        "sy" -> com.hbb20.R.drawable.flag_syria
        "sz" -> com.hbb20.R.drawable.flag_swaziland
        "tc" -> com.hbb20.R.drawable.flag_turks_and_caicos_islands
        "td" -> com.hbb20.R.drawable.flag_chad
        "tg" -> com.hbb20.R.drawable.flag_togo
        "th" -> com.hbb20.R.drawable.flag_thailand
        "tj" -> com.hbb20.R.drawable.flag_tajikistan
        "tk" -> com.hbb20.R.drawable.flag_tokelau // custom
        "tl" -> com.hbb20.R.drawable.flag_timor_leste
        "tm" -> com.hbb20.R.drawable.flag_turkmenistan
        "tn" -> com.hbb20.R.drawable.flag_tunisia
        "to" -> com.hbb20.R.drawable.flag_tonga
        "tr" -> com.hbb20.R.drawable.flag_turkey
        "tt" -> com.hbb20.R.drawable.flag_trinidad_and_tobago
        "tv" -> com.hbb20.R.drawable.flag_tuvalu
        "tw" -> com.hbb20.R.drawable.flag_taiwan
        "tz" -> com.hbb20.R.drawable.flag_tanzania
        "ua" -> com.hbb20.R.drawable.flag_ukraine
        "ug" -> com.hbb20.R.drawable.flag_uganda
        "us" -> com.hbb20.R.drawable.flag_united_states_of_america
        "uy" -> com.hbb20.R.drawable.flag_uruguay
        "uz" -> com.hbb20.R.drawable.flag_uzbekistan
        "va" -> com.hbb20.R.drawable.flag_vatican_city
        "vc" -> com.hbb20.R.drawable.flag_saint_vicent_and_the_grenadines
        "ve" -> com.hbb20.R.drawable.flag_venezuela
        "vg" -> com.hbb20.R.drawable.flag_british_virgin_islands
        "vi" -> com.hbb20.R.drawable.flag_us_virgin_islands
        "vn" -> com.hbb20.R.drawable.flag_vietnam
        "vu" -> com.hbb20.R.drawable.flag_vanuatu
        "wf" -> com.hbb20.R.drawable.flag_wallis_and_futuna
        "ws" -> com.hbb20.R.drawable.flag_samoa
        "xk" -> com.hbb20.R.drawable.flag_kosovo
        "ye" -> com.hbb20.R.drawable.flag_yemen
        "yt" -> com.hbb20.R.drawable.flag_martinique // no exact flag found
        "za" -> com.hbb20.R.drawable.flag_south_africa
        "zm" -> com.hbb20.R.drawable.flag_zambia
        "zw" -> com.hbb20.R.drawable.flag_zimbabwe
        else -> com.hbb20.R.drawable.flag_transparent
    }
}
