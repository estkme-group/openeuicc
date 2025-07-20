package im.angry.openeuicc.util

interface UnprivilegedEuiccContextMarker : OpenEuiccContextMarker {
    override val preferenceRepository: UnprivilegedPreferenceRepository
        get() = appContainer.preferenceRepository as UnprivilegedPreferenceRepository
}