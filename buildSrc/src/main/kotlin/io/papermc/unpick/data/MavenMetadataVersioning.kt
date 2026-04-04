package io.papermc.unpick.data

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("versioning")
data class MavenMetadataVersioning(
    @XmlChildrenName("version")
    val versions: List<String>,
)
