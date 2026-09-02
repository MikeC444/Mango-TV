package tv.mango.app.addon

import kotlinx.serialization.json.JsonObject
import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.model.AddonManifest
import tv.mango.app.addon.protocol.AddonManifestParser
import tv.mango.app.addon.protocol.AddonUrls
import tv.mango.app.addon.protocol.ProtocolFailure
import tv.mango.app.addon.protocol.StremioProtocolClient

/**
 * Fetching, checking and installing an add-on from a manifest URL.
 *
 * Installation is deliberately two steps. [preview] fetches and validates, and
 * hands back what the add-on says about itself so the viewer can see its name,
 * its logo and what it can actually do before agreeing to anything. Only
 * [install] writes. An add-on is an untrusted remote service, and installing
 * one sight-unseen because a URL was pasted is not a decision the application
 * should make on the viewer's behalf.
 */
class AddonInstaller(
    private val client: StremioProtocolClient,
    private val store: AddonStore,
    private val now: () -> Long = System::currentTimeMillis,
) {

    sealed interface Preview {
        /** Ready to install. */
        data class Ready(
            val manifest: AddonManifest,
            val manifestUrl: String,
            val manifestJson: String,
            val alreadyInstalled: Boolean,
        ) : Preview

        /**
         * Valid, but the add-on says it will not work until configured.
         *
         * Carries the page the viewer configures it on. Configuring produces a
         * new manifest URL, which is then installed like any other.
         */
        data class NeedsConfiguration(
            val manifest: AddonManifest,
            val manifestUrl: String,
            val configureUrl: String,
        ) : Preview

        data class Failed(val reason: Reason) : Preview

        enum class Reason {
            /** Not a URL this application can fetch. */
            INVALID_URL,

            /** Nothing answered, or the answer never arrived. */
            UNREACHABLE,

            /** Something answered, but not with an add-on manifest. */
            NOT_AN_ADDON,

            /** A manifest, but missing an id, a name or a version. */
            INCOMPLETE_MANIFEST,

            /** A manifest offering nothing this application can use. */
            NOTHING_USABLE,
        }
    }

    /** Fetches and validates, without writing anything. */
    suspend fun preview(input: String): Preview {
        val manifestUrl = AddonUrls.manifestUrl(input)
            ?: return Preview.Failed(Preview.Reason.INVALID_URL)

        val outcome = client.fetch(manifestUrl)
        val body: JsonObject = when (outcome) {
            is StremioProtocolClient.Outcome.Success -> outcome.body
            is StremioProtocolClient.Outcome.Failure -> return Preview.Failed(
                when (outcome.reason) {
                    ProtocolFailure.MALFORMED, ProtocolFailure.EMPTY_BODY ->
                        Preview.Reason.NOT_AN_ADDON
                    else -> Preview.Reason.UNREACHABLE
                },
            )
        }

        return when (val parsed = AddonManifestParser.parse(body)) {
            is AddonManifestParser.Result.Valid -> ready(parsed.manifest, manifestUrl, body)
            is AddonManifestParser.Result.Invalid -> Preview.Failed(
                when (parsed.reason) {
                    AddonManifestParser.InvalidReason.MISSING_REQUIRED_FIELDS ->
                        Preview.Reason.INCOMPLETE_MANIFEST
                    AddonManifestParser.InvalidReason.NO_USABLE_RESOURCES ->
                        Preview.Reason.NOTHING_USABLE
                    else -> Preview.Reason.NOT_AN_ADDON
                },
            )
        }
    }

    private suspend fun ready(
        manifest: AddonManifest,
        manifestUrl: String,
        body: JsonObject,
    ): Preview {
        if (manifest.behaviorHints.configurationRequired) {
            return Preview.NeedsConfiguration(
                manifest = manifest,
                manifestUrl = manifestUrl,
                configureUrl = configureUrl(manifestUrl),
            )
        }
        return Preview.Ready(
            manifest = manifest,
            manifestUrl = manifestUrl,
            manifestJson = body.toString(),
            alreadyInstalled = store.installed().any { it.id == manifest.id },
        )
    }

    /** Commits a previewed add-on. */
    suspend fun install(preview: Preview.Ready) {
        store.install(
            Addon(
                manifestUrl = preview.manifestUrl,
                manifest = preview.manifest,
                manifestJson = preview.manifestJson,
                isEnabled = true,
                installedAtMillis = now(),
                manifestRefreshedAtMillis = now(),
            ),
        )
    }

    /**
     * Re-fetches a manifest for an add-on already installed.
     *
     * An add-on's capabilities change when its author changes them, and a
     * stored manifest would otherwise describe it as it was on the day it was
     * installed. Failure is silent: the stored manifest keeps working, and
     * bothering the viewer about a refresh they did not ask for would be noise.
     */
    suspend fun refresh(addon: Addon): Boolean {
        val outcome = client.fetch(addon.manifestUrl)
        val body = (outcome as? StremioProtocolClient.Outcome.Success)?.body ?: return false
        val manifest = (AddonManifestParser.parse(body) as? AddonManifestParser.Result.Valid)
            ?.manifest ?: return false

        store.updateManifest(
            addonId = addon.id,
            addon = addon.copy(
                manifest = manifest,
                manifestJson = body.toString(),
                manifestRefreshedAtMillis = now(),
            ),
        )
        return true
    }

    /**
     * Where an add-on is configured.
     *
     * By convention the page sits beside the manifest at /configure, and
     * completing it produces a new manifest URL carrying the configuration in
     * its path.
     */
    fun configureUrl(manifestUrl: String): String =
        manifestUrl.removeSuffix(Addon.MANIFEST_SUFFIX).trimEnd('/') + "/configure"
}
