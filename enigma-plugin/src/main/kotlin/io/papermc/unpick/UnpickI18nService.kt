package io.papermc.unpick

import cuchaz.enigma.api.service.I18nService
import java.io.InputStream

class UnpickI18nService : I18nService {
    companion object {
        const val COPY_TARGET_REFERENCE = "unpick.copyTargetReference"
        const val COPY_CONSTANT_REFERENCE = "unpick.copyConstantReference"
    }

    override fun getTranslationResource(language: String): InputStream? {
        return javaClass.classLoader.getResourceAsStream("unpick_lang/$language.json")
    }
}
